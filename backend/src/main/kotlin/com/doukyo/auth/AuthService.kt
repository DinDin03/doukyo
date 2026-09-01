package com.doukyo.auth

import com.doukyo.common.UnauthorizedException
import com.doukyo.user.User
import com.doukyo.user.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Base64

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${doukyo.security.refresh-ttl-days}") private val refreshTtlDays: Long,
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun signUp(name: String, email: String, password: String): AuthPayload {
        val cleanEmail = email.trim().lowercase()
        require(name.isNotBlank()) { "Please enter your name" }
        require(password.length >= 8) { "Password must be at least 8 characters" }
        require(!userRepository.existsByEmail(cleanEmail)) { "An account with that email already exists" }

        // encode() = BCrypt hash. The plaintext password is never stored or logged.
        val user = userRepository.save(
            User(name = name.trim(), email = cleanEmail, passwordHash = passwordEncoder.encode(password)),
        )
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

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return Base64.getEncoder().encodeToString(digest)
    }
}
