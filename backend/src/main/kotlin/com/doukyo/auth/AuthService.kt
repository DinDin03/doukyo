package com.doukyo.auth

import com.doukyo.common.UnauthorizedException
import com.doukyo.user.User
import com.doukyo.user.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val oAuthAccountRepository: OAuthAccountRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val googleTokenVerifier: GoogleTokenVerifier,
    private val pendingSignUpRepository: PendingSignUpRepository,
    private val emailSender: EmailSender,
    @Value("\${doukyo.security.refresh-ttl-days}") private val refreshTtlDays: Long,
) {
    private val secureRandom = SecureRandom()

    companion object {
        const val MAX_CODE_ATTEMPTS = 5
        const val LOCKOUT_MINUTES = 5L
        const val CODE_TTL_MINUTES = 10L
        const val RESEND_INTERVAL_SECONDS = 60L
        // One message for every failure mode in confirmSignUp.
        const val INVALID_CODE = "That code is invalid or has expired"
    }

    // STEP ONE. Emails a code and returns true — ALWAYS true, whether or not the
    // address already has an account. Answering differently would turn the sign-up
    // form into an account-enumeration oracle, the same reason signIn has a single
    // generic error. The difference goes in the email instead, where only the owner
    // of the inbox can read it.
    @Transactional
    fun startSignUp(name: String, email: String, password: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        // The caller's own input — rejecting it leaks nothing about other accounts.
        require(name.isNotBlank()) { "Please enter your name" }
        require(cleanEmail.contains("@")) { "Please enter a valid email" }
        require(password.length >= 8) { "Password must be at least 8 characters" }

        if (userRepository.existsByEmail(cleanEmail)) {
            pendingSignUpRepository.findByEmail(cleanEmail)?.let(pendingSignUpRepository::delete)
            afterCommit { emailSender.sendSignUpAttemptOnExistingAccount(cleanEmail) }
            return true
        }

        val existing = pendingSignUpRepository.findByEmail(cleanEmail)

        // A locked record issues no new code. Without this the lockout is
        // meaningless: the caller taps resend and gets a fresh code plus a fresh
        // set of attempts. Still returns true — the response stays uniform.
        if (existing?.lockedUntil?.isAfter(OffsetDateTime.now()) == true) return true

        // Throttle resends: without this we are an email cannon aimed at whatever
        // address the caller types.
        if (existing != null && existing.createdAt.isAfter(OffsetDateTime.now().minusSeconds(RESEND_INTERVAL_SECONDS))) {
            return true
        }

        val code = randomCode()
        val now = OffsetDateTime.now()
        // Replacing rather than adding keeps exactly one code live per address, so
        // an attacker cannot collect candidates and try them in parallel.
        val pending = existing ?: PendingSignUp(
            email = cleanEmail,
            name = name.trim(),
            passwordHash = "",
            codeHash = "",
            expiresAt = now,
        )
        pending.name = name.trim()
        pending.passwordHash = passwordEncoder.encode(password)
        pending.codeHash = sha256(code)
        pending.attempts = 0
        pending.lockedUntil = null
        pending.createdAt = now
        pending.expiresAt = now.plusMinutes(CODE_TTL_MINUTES)
        pendingSignUpRepository.save(pending)

        afterCommit { emailSender.sendSignUpCode(cleanEmail, code) }
        return true
    }

    // STEP TWO. The code is the credential, so this needs no existing session.
    //
    // noRollbackFor is load-bearing, not tidiness: a wrong code throws, and a
    // throw inside @Transactional rolls the transaction back — including the
    // attempts increment. That silently disables the guess limit, which is the
    // ONLY thing protecting a code with a million possibilities. The failed
    // attempt has to survive the failure.
    @Transactional(noRollbackFor = [IllegalArgumentException::class])
    fun confirmSignUp(email: String, code: String): AuthPayload {
        val cleanEmail = email.trim().lowercase()
        val pending = pendingSignUpRepository.findByEmail(cleanEmail)
            ?: throw IllegalArgumentException(INVALID_CODE)

        val now = OffsetDateTime.now()

        // Locked from earlier failures: say how long is left rather than looking
        // like a wrong code, or the user keeps guessing into a wall.
        pending.lockedUntil?.let { until ->
            if (until.isAfter(now)) {
                val minutes = ChronoUnit.MINUTES.between(now, until) + 1
                throw IllegalArgumentException("Too many attempts. Try again in $minutes minute${if (minutes == 1L) "" else "s"}.")
            }
        }

        if (pending.expiresAt.isBefore(now)) {
            pendingSignUpRepository.delete(pending)
            throw IllegalArgumentException(INVALID_CODE)
        }

        // Six digits is a million possibilities — the ATTEMPT LIMIT is what makes
        // that safe, not the digits and not the hash.
        if (pending.codeHash != sha256(code.trim())) {
            pending.attempts += 1
            val left = MAX_CODE_ATTEMPTS - pending.attempts
            if (left <= 0) {
                // Keep the row: it carries the deadline. Deleting it would hand the
                // caller a clean slate on the next startSignUp.
                pending.lockedUntil = now.plusMinutes(LOCKOUT_MINUTES)
                throw IllegalArgumentException("Too many attempts. Try again in $LOCKOUT_MINUTES minutes.")
            }
            // Telling the user how many tries remain reveals that a sign-up is in
            // flight for this address. Accepted deliberately: guessing burns the
            // budget, so probing is self-limiting, and a silent wall is a far worse
            // experience for the person who simply mistyped.
            throw IllegalArgumentException("Incorrect code — $left attempt${if (left == 1) "" else "s"} left")
        }

        // Safe to be specific: holding a valid code already proves control of the
        // inbox, so this reveals nothing they could not learn anyway.
        require(!userRepository.existsByEmail(cleanEmail)) { "An account with that email already exists" }

        val user = userRepository.save(
            User(
                name = pending.name,
                email = cleanEmail,
                passwordHash = pending.passwordHash,
                emailVerified = true,
            ),
        )
        pendingSignUpRepository.delete(pending) // single use
        return issueTokens(user)
    }

    @Transactional
    fun signIn(email: String, password: String): AuthPayload {
        val user = userRepository.findByEmail(email.trim().lowercase())
        val hash = user?.passwordHash
        // ONE generic error for "no such user" and "wrong password" — never reveal
        // whether an email is registered. matches() re-hashes the input and compares.
        if (hash == null || !passwordEncoder.matches(password, hash)) {
            throw IllegalArgumentException("Invalid email or password")
        }
        return issueTokens(user)
    }

    @Transactional
    fun refresh(rawRefreshToken: String): AuthPayload {
        val stored = refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
        if (stored == null || stored.revokedAt != null || stored.expiresAt.isBefore(OffsetDateTime.now())) {
            throw UnauthorizedException("Your session has expired — please sign in again")
        }
        // ROTATION: spend this refresh token so it can't be reused, then mint a fresh pair.
        stored.revokedAt = OffsetDateTime.now()
        return issueTokens(stored.user)
    }

    // Revokes a refresh token server-side, so a copy that leaked before sign-out
    // can't be used to get a new access token. Always succeeds — an unknown or
    // already-revoked token is not an error, just a no-op.
    @Transactional
    fun signOut(rawRefreshToken: String) {
        refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
            ?.takeIf { it.revokedAt == null }
            ?.let { it.revokedAt = OffsetDateTime.now() }
    }

    @Transactional
    fun googleSignIn(idToken: String): AuthPayload {
        // The ONE place we decide to trust the caller's claim of identity. Everything
        // below treats `identity` as ground truth.
        val identity = googleTokenVerifier.verify(idToken)

        // Case 1: this Google account has signed in before — same user, every time.
        // (provider, providerUserId) is unique, so at most one row can match.
        oAuthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, identity.subject)?.let {
            return issueTokens(it.user)
        }

        val cleanEmail = identity.email.trim().lowercase()
        val existingByEmail = userRepository.findByEmail(cleanEmail)

        val user = if (existingByEmail != null) {
            // Case 2: an account with this email already exists (likely password-based).
            // LINK to it — but ONLY because Google itself asserts the email is verified.
            // Linking on an unverified email would let an attacker take over any
            // account just by typing its address into a Google account they control.
            require(identity.emailVerified) {
                "This Google account's email isn't verified — sign in with your password instead"
            }
            // The email is now proven; upgrade the local flag if it wasn't already.
            if (!existingByEmail.emailVerified) existingByEmail.emailVerified = true
            userRepository.save(existingByEmail)
        } else {
            // Case 3: brand-new user, Google-only. No password — passwordHash stays
            // null, exactly the case that column was made nullable for.
            userRepository.save(
                User(name = identity.name, email = cleanEmail, emailVerified = identity.emailVerified),
            )
        }

        // Remember this Google identity so case 1 fires on every future sign-in.
        oAuthAccountRepository.save(
            OAuthAccount(user = user, provider = OAuthProvider.GOOGLE, providerUserId = identity.subject),
        )

        return issueTokens(user)
    }

    // Mint an access JWT + a new refresh token; persist only the refresh token's hash.
    private fun issueTokens(user: User): AuthPayload {
        val accessToken = jwtService.issueAccessToken(user.id!!)
        val rawRefresh = randomToken()
        refreshTokenRepository.save(
            RefreshToken(
                user = user,
                tokenHash = sha256(rawRefresh),
                expiresAt = OffsetDateTime.now().plusDays(refreshTtlDays),
            ),
        )
        return AuthPayload(accessToken = accessToken, refreshToken = rawRefresh, user = user)
    }

    // 32 cryptographically-random bytes, URL-safe. Unguessable, so we can store just
    // a fast SHA-256 hash of it (bcrypt's slowness is only needed for weak passwords).
    private fun randomToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    // SMTP is a network call; it must not run inside the transaction. Holding a
    // pooled connection open for an SMTP round trip starves the pool, and a
    // rollback after the send would email a code for a sign-up that no longer
    // exists. Same shape as the chat publish fix.
    private fun afterCommit(action: () -> Unit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action()
            return
        }
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() = action()
            },
        )
    }

    private fun randomCode(): String = (1..6).map { secureRandom.nextInt(10) }.joinToString("")

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return Base64.getEncoder().encodeToString(digest)
    }
}
