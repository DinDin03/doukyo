package com.doukyo.auth

import com.doukyo.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.OffsetDateTime

// Sign-up is two steps: startSignUp emails a code, confirmSignUp exchanges it for
// an account. No user row exists until the code comes back.
class EmailVerificationTest : AbstractIntegrationTest() {

    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var pendingSignUpRepository: PendingSignUpRepository

    @MockBean private lateinit var googleTokenVerifier: GoogleTokenVerifier

    private fun completeSignUp(name: String, email: String, password: String): AuthPayload {
        authService.startSignUp(name, email, password)
        return authService.confirmSignUp(email, codeSentTo(email))
    }

    // --- startSignUp ---

    @Test
    fun `starting a sign-up creates no user, only a pending record`() {
        authService.startSignUp("Alice", "alice@test.app", "password123")

        assertThat(userRepository.findAll()).isEmpty()
        assertThat(pendingSignUpRepository.findByEmail("alice@test.app")).isNotNull()
    }

    @Test
    fun `starting a sign-up emails a six digit code`() {
        authService.startSignUp("Alice", "alice@test.app", "password123")
        assertThat(codeSentTo("alice@test.app")).matches("\\d{6}")
    }

    @Test
    fun `the code is never stored in plaintext`() {
        authService.startSignUp("Alice", "alice@test.app", "password123")
        val stored = pendingSignUpRepository.findByEmail("alice@test.app")!!

        assertThat(stored.codeHash).isNotEqualTo(codeSentTo("alice@test.app"))
        assertThat(stored.passwordHash).isNotEqualTo("password123")
    }

    @Test
    fun `an existing account gets the same response but a different email`() {
        completeSignUp("Alice", "alice@test.app", "password123")
        emailSender.clear()

        // Identical return value — the sign-up form must not become an
        // account-enumeration oracle.
        assertThat(authService.startSignUp("Impostor", "alice@test.app", "password123")).isTrue()

        // No code goes out; the real owner is told someone tried instead.
        assertThat(emailSender.codes).isEmpty()
        assertThat(emailSender.existingAccountNotices).containsExactly("alice@test.app")
        assertThat(pendingSignUpRepository.findByEmail("alice@test.app")).isNull()
    }

    @Test
    fun `starting a sign-up returns true for a brand new email too`() {
        assertThat(authService.startSignUp("Alice", "alice@test.app", "password123")).isTrue()
    }

    @Test
    fun `start rejects a blank name and a short password`() {
        // These are the caller's OWN input — rejecting them leaks nothing and
        // saves a pointless round trip through the inbox.
        assertThatThrownBy { authService.startSignUp("", "a@test.app", "password123") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { authService.startSignUp("Alice", "a@test.app", "short") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `start normalizes the email`() {
        authService.startSignUp("Alice", "  ALICE@Test.App  ", "password123")
        assertThat(pendingSignUpRepository.findByEmail("alice@test.app")).isNotNull()
    }

    @Test
    fun `requesting again replaces the previous code`() {
        authService.startSignUp("Alice", "alice@test.app", "password123")
        val first = codeSentTo("alice@test.app")

        // Age the record past the resend throttle so a fresh code is issued.
        val pending = pendingSignUpRepository.findByEmail("alice@test.app")!!
        pending.createdAt = OffsetDateTime.now().minusMinutes(5)
        pendingSignUpRepository.save(pending)
        emailSender.clear()

        authService.startSignUp("Alice", "alice@test.app", "password123")
        val second = codeSentTo("alice@test.app")

        assertThat(second).isNotEqualTo(first)
        // Only one code may be live at a time, or an attacker collects candidates.
        assertThatThrownBy { authService.confirmSignUp("alice@test.app", first) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(authService.confirmSignUp("alice@test.app", second).user.email).isEqualTo("alice@test.app")
    }

    @Test
    fun `a rapid second request does not send another email`() {
        authService.startSignUp("Alice", "alice@test.app", "password123")
        authService.startSignUp("Alice", "alice@test.app", "password123")

        // Throttled: still returns true, but we are not an email cannon.
        assertThat(emailSender.sendCount).isEqualTo(1)
    }

    // --- confirmSignUp ---

    @Test
    fun `the right code creates a verified user and returns tokens`() {
        val payload = completeSignUp("Alice", "alice@test.app", "password123")

        assertThat(payload.accessToken).isNotBlank()
        assertThat(payload.refreshToken).isNotBlank()
        assertThat(payload.user.emailVerified).isTrue()
        assertThat(userRepository.findByEmail("alice@test.app")).isNotNull()
        // The pending record is consumed, so the code cannot be replayed.
        assertThat(pendingSignUpRepository.findByEmail("alice@test.app")).isNull()
    }

    @Test
    fun `the new account can sign in with its password`() {
        completeSignUp("Alice", "alice@test.app", "password123")
        assertThat(authService.signIn("alice@test.app", "password123").user.name).isEqualTo("Alice")
    }

    @Test
    fun `a wrong code is refused and creates nothing`() {
        authService.startSignUp("Alice", "alice@test.app", "password123")

        assertThatThrownBy { authService.confirmSignUp("alice@test.app", "000000") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(userRepository.findAll()).isEmpty()
    }

    @Test
    fun `every failure gives the same message`() {
        authService.startSignUp("Alice", "alice@test.app", "password123")
        val expired = pendingSignUpRepository.findByEmail("alice@test.app")!!
        expired.expiresAt = OffsetDateTime.now().minusMinutes(1)
        pendingSignUpRepository.save(expired)

        // Wrong code, expired code and no pending sign-up at all must be
        // indistinguishable, or the message tells an attacker what to try next.
        val expiredMessage = runCatching { authService.confirmSignUp("alice@test.app", "000000") }
            .exceptionOrNull()!!.message
        val unknownMessage = runCatching { authService.confirmSignUp("nobody@test.app", "000000") }
            .exceptionOrNull()!!.message

        assertThat(expiredMessage).isEqualTo(unknownMessage)
    }

    @Test
    fun `an expired code is refused`() {
        authService.startSignUp("Alice", "alice@test.app", "password123")
        val code = codeSentTo("alice@test.app")
        val pending = pendingSignUpRepository.findByEmail("alice@test.app")!!
        pending.expiresAt = OffsetDateTime.now().minusSeconds(1)
        pendingSignUpRepository.save(pending)

        assertThatThrownBy { authService.confirmSignUp("alice@test.app", code) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(userRepository.findAll()).isEmpty()
    }

    @Test
    fun `a code works only once`() {
        authService.startSignUp("Alice", "alice@test.app", "password123")
        val code = codeSentTo("alice@test.app")
        authService.confirmSignUp("alice@test.app", code)

        assertThatThrownBy { authService.confirmSignUp("alice@test.app", code) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(userRepository.findAll()).hasSize(1)
    }

    @Test
    fun `too many wrong guesses destroys the code even if the next guess is right`() {
        // The guess limit — not the 6 digits — is what actually protects a code
        // with only a million possibilities.
        authService.startSignUp("Alice", "alice@test.app", "password123")
        val code = codeSentTo("alice@test.app")

        repeat(AuthService.MAX_CODE_ATTEMPTS) {
            runCatching { authService.confirmSignUp("alice@test.app", "000000") }
        }

        assertThatThrownBy { authService.confirmSignUp("alice@test.app", code) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(userRepository.findAll()).isEmpty()
        assertThat(pendingSignUpRepository.findByEmail("alice@test.app")).isNull()
    }

    @Test
    fun `wrong guesses below the limit leave the real code usable`() {
        authService.startSignUp("Alice", "alice@test.app", "password123")
        val code = codeSentTo("alice@test.app")

        repeat(AuthService.MAX_CODE_ATTEMPTS - 1) {
            runCatching { authService.confirmSignUp("alice@test.app", "000000") }
        }

        assertThat(authService.confirmSignUp("alice@test.app", code).user.name).isEqualTo("Alice")
    }

    @Test
    fun `an email claimed between start and confirm is reported clearly`() {
        authService.startSignUp("Alice", "alice@test.app", "password123")
        val code = codeSentTo("alice@test.app")

        // Someone else finished first — safe to be specific here, since holding a
        // valid code already proves control of the inbox.
        given(googleTokenVerifier.verify("tok")).willReturn(
            GoogleIdentity("g-1", "alice@test.app", emailVerified = true, name = "Alice"),
        )
        authService.googleSignIn("tok")

        assertThatThrownBy { authService.confirmSignUp("alice@test.app", code) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("already exists")
    }
}
