package com.doukyo.auth

import com.doukyo.AbstractIntegrationTest
import com.doukyo.common.UnauthorizedException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.OffsetDateTime

class AuthServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired private lateinit var oAuthAccountRepository: OAuthAccountRepository
    @Autowired private lateinit var passwordEncoder: PasswordEncoder

    // Google's servers are not reachable from a test, and we must be able to assert
    // on identities we control (verified vs unverified email).
    @MockBean private lateinit var googleTokenVerifier: GoogleTokenVerifier

    // Sign-up is two steps now, so the fixture completes both.
    private fun signUp(name: String, email: String, password: String): AuthPayload {
        authService.startSignUp(name, email, password)
        return authService.confirmSignUp(email, codeSentTo(email))
    }

    private fun google(sub: String, email: String, verified: Boolean = true, name: String = "Googler") =
        GoogleIdentity(subject = sub, email = email, emailVerified = verified, name = name)

    // --- signUp ---

    @Test
    fun `signUp creates the user and returns both tokens`() {
        val payload = signUp("Alice", "alice@test.app", "password123")

        assertThat(payload.user.name).isEqualTo("Alice")
        assertThat(payload.accessToken).isNotBlank()
        assertThat(payload.refreshToken).isNotBlank()
        assertThat(payload.accessToken).isNotEqualTo(payload.refreshToken)
    }

    @Test
    fun `signUp stores a bcrypt hash, never the plaintext`() {
        val payload = signUp("Alice", "alice@test.app", "password123")
        val hash = userRepository.findByEmail("alice@test.app")!!.passwordHash!!

        assertThat(hash).isNotEqualTo("password123")
        assertThat(hash).startsWith("\$2")
        assertThat(passwordEncoder.matches("password123", hash)).isTrue()
        assertThat(payload.user.emailVerified).isTrue()
    }

    @Test
    fun `signUp normalizes the email and trims the name`() {
        signUp("  Alice  ", "  ALICE@Test.App  ", "password123")
        assertThat(userRepository.findByEmail("alice@test.app")?.name).isEqualTo("Alice")
    }

    @Test
    fun `signUp rejects a blank name`() {
        listOf("", "   ").forEach {
            assertThatThrownBy { signUp(it, "a@test.app", "password123") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Please enter your name")
        }
    }

    @Test
    fun `signUp rejects a password shorter than 8 characters`() {
        assertThatThrownBy { signUp("Alice", "a@test.app", "1234567") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Password must be at least 8 characters")
    }

    @Test
    fun `signUp accepts a password of exactly 8 characters`() {
        assertThat(signUp("Alice", "a@test.app", "12345678").accessToken).isNotBlank()
    }

    // --- signIn ---

    @Test
    fun `signIn succeeds with the right password`() {
        signUp("Alice", "alice@test.app", "password123")
        assertThat(authService.signIn("alice@test.app", "password123").user.name).isEqualTo("Alice")
    }

    @Test
    fun `signIn is case and whitespace insensitive on email`() {
        signUp("Alice", "alice@test.app", "password123")
        assertThat(authService.signIn("  ALICE@Test.App ", "password123").accessToken).isNotBlank()
    }

    @Test
    fun `signIn gives one generic error for wrong password and unknown email alike`() {
        signUp("Alice", "alice@test.app", "password123")

        // Identical messages: the login form must not become an account-enumeration oracle.
        assertThatThrownBy { authService.signIn("alice@test.app", "wrong-password") }
            .hasMessage("Invalid email or password")
        assertThatThrownBy { authService.signIn("nobody@test.app", "password123") }
            .hasMessage("Invalid email or password")
    }

    @Test
    fun `signIn rejects a Google-only account that has no password`() {
        given(googleTokenVerifier.verify("tok")).willReturn(google("g-1", "googler@test.app"))
        authService.googleSignIn("tok")

        assertThatThrownBy { authService.signIn("googler@test.app", "anything") }
            .hasMessage("Invalid email or password")
    }

    // --- refresh ---

    @Test
    fun `refresh issues a new pair`() {
        val first = signUp("Alice", "alice@test.app", "password123")
        val second = authService.refresh(first.refreshToken)

        assertThat(second.refreshToken).isNotEqualTo(first.refreshToken)
        assertThat(second.user.id).isEqualTo(first.user.id)
    }

    @Test
    fun `refresh rotates - the spent token cannot be reused`() {
        val first = signUp("Alice", "alice@test.app", "password123")
        authService.refresh(first.refreshToken)

        assertThatThrownBy { authService.refresh(first.refreshToken) }
            .isInstanceOf(UnauthorizedException::class.java)
            .hasMessageContaining("session has expired")
    }

    @Test
    fun `refresh rejects an unknown token`() {
        assertThatThrownBy { authService.refresh("never-issued") }
            .isInstanceOf(UnauthorizedException::class.java)
    }

    @Test
    fun `refresh rejects an expired token`() {
        val user = newUser("Expired")
        // The raw token is never stored, only its hash — so build the row directly.
        val raw = "raw-expired-token"
        refreshTokenRepository.save(
            RefreshToken(
                user = user,
                tokenHash = sha256(raw),
                expiresAt = OffsetDateTime.now().minusDays(1),
            ),
        )
        assertThatThrownBy { authService.refresh(raw) }
            .isInstanceOf(UnauthorizedException::class.java)
    }

    // --- signOut ---

    @Test
    fun `signOut revokes the refresh token`() {
        val payload = signUp("Alice", "alice@test.app", "password123")
        authService.signOut(payload.refreshToken)

        assertThatThrownBy { authService.refresh(payload.refreshToken) }
            .isInstanceOf(UnauthorizedException::class.java)
    }

    @Test
    fun `signOut is idempotent and never errors on an unknown token`() {
        val payload = signUp("Alice", "alice@test.app", "password123")
        authService.signOut(payload.refreshToken)
        authService.signOut(payload.refreshToken) // already revoked
        authService.signOut("never-issued")       // unknown
    }

    @Test
    fun `signOut does not revoke other sessions`() {
        val phone = signUp("Alice", "alice@test.app", "password123")
        val laptop = authService.signIn("alice@test.app", "password123")

        authService.signOut(phone.refreshToken)

        assertThat(authService.refresh(laptop.refreshToken).accessToken).isNotBlank()
    }

    // --- googleSignIn ---

    @Test
    fun `googleSignIn creates a passwordless account for a new identity`() {
        given(googleTokenVerifier.verify("tok")).willReturn(google("g-1", "new@test.app", name = "New Person"))

        val payload = authService.googleSignIn("tok")

        assertThat(payload.user.name).isEqualTo("New Person")
        assertThat(userRepository.findByEmail("new@test.app")!!.passwordHash).isNull()
        assertThat(oAuthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "g-1")).isNotNull()
    }

    @Test
    fun `googleSignIn returns the same user on every subsequent sign-in`() {
        given(googleTokenVerifier.verify("tok")).willReturn(google("g-1", "new@test.app"))

        val first = authService.googleSignIn("tok")
        val second = authService.googleSignIn("tok")

        assertThat(second.user.id).isEqualTo(first.user.id)
        assertThat(userRepository.findAll()).hasSize(1)
    }

    @Test
    fun `googleSignIn links to an existing password account when Google verified the email`() {
        val existing = signUp("Alice", "alice@test.app", "password123")
        given(googleTokenVerifier.verify("tok")).willReturn(google("g-1", "alice@test.app", verified = true))

        val linked = authService.googleSignIn("tok")

        assertThat(linked.user.id).isEqualTo(existing.user.id)
        assertThat(userRepository.findAll()).hasSize(1)
        // The address is now proven, so the local flag is upgraded.
        assertThat(userRepository.findByEmail("alice@test.app")!!.emailVerified).isTrue()
        // The password still works — linking adds a way in, it doesn't replace one.
        assertThat(authService.signIn("alice@test.app", "password123").accessToken).isNotBlank()
    }

    @Test
    fun `googleSignIn refuses to link on an unverified email`() {
        signUp("Alice", "alice@test.app", "password123")
        given(googleTokenVerifier.verify("tok")).willReturn(google("g-1", "alice@test.app", verified = false))

        // Otherwise anyone could claim a victim's address on a Google account they
        // control and be handed the victim's existing account.
        assertThatThrownBy { authService.googleSignIn("tok") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("isn't verified")
    }

    @Test
    fun `googleSignIn propagates a rejected token`() {
        given(googleTokenVerifier.verify("bad"))
            .willThrow(IllegalArgumentException("Invalid Google sign-in"))

        assertThatThrownBy { authService.googleSignIn("bad") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(userRepository.findAll()).isEmpty()
    }

    @Test
    fun `googleSignIn normalizes the email it receives`() {
        given(googleTokenVerifier.verify("tok")).willReturn(google("g-1", "  MiXeD@Test.App "))
        authService.googleSignIn("tok")
        assertThat(userRepository.findByEmail("mixed@test.app")).isNotNull()
    }

    // Mirrors AuthService's private hashing so an expired row can be planted.
    private fun sha256(value: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return java.util.Base64.getEncoder().encodeToString(digest)
    }
}
