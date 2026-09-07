package com.doukyo.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Base64

// Pure unit test — no Spring context, no database. JwtService takes its config as
// constructor args, so it can just be built.
class JwtServiceTest {

    private fun secret(seed: String) =
        Base64.getEncoder().encodeToString(seed.padEnd(48, '!').toByteArray())

    private val jwt = JwtService(secret("doukyo-test-secret"), accessTtlMinutes = 15)

    @Test
    fun `round-trips the user id`() {
        val token = jwt.issueAccessToken(42L)
        assertThat(jwt.parseUserId(token)).isEqualTo(42L)
    }

    @Test
    fun `issues a three-part JWT`() {
        assertThat(jwt.issueAccessToken(1L).split(".")).hasSize(3)
    }

    @Test
    fun `different users get different tokens`() {
        assertThat(jwt.issueAccessToken(1L)).isNotEqualTo(jwt.issueAccessToken(2L))
    }

    @Test
    fun `rejects garbage`() {
        assertThat(jwt.parseUserId("not-a-token")).isNull()
        assertThat(jwt.parseUserId("")).isNull()
        assertThat(jwt.parseUserId("a.b.c")).isNull()
    }

    @Test
    fun `rejects a tampered payload`() {
        val token = jwt.issueAccessToken(1L)
        val parts = token.split(".")
        // Re-encode the payload claiming to be user 999; the signature no longer matches.
        val forged = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"sub":"999"}""".toByteArray())
        assertThat(jwt.parseUserId("${parts[0]}.$forged.${parts[2]}")).isNull()
    }

    @Test
    fun `rejects a token signed with a different key`() {
        val other = JwtService(secret("a-completely-different-key"), accessTtlMinutes = 15)
        assertThat(jwt.parseUserId(other.issueAccessToken(1L))).isNull()
    }

    @Test
    fun `rejects an expired token`() {
        // Negative TTL puts `exp` in the past the moment it is issued.
        val expiring = JwtService(secret("doukyo-test-secret"), accessTtlMinutes = -1)
        assertThat(jwt.parseUserId(expiring.issueAccessToken(1L))).isNull()
    }
}
