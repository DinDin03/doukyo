package com.doukyo.auth

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date

// Creates and verifies access tokens (JWTs). The same secret key both signs and
// verifies (HMAC / symmetric), which is exactly why the secret never leaves the
// server — anyone with it could mint valid tokens.
@Service
class JwtService(
    @Value("\${doukyo.security.jwt-secret}") secret: String,
    @Value("\${doukyo.security.access-ttl-minutes}") private val accessTtlMinutes: Long,
) {
    private val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))

    // Issue a short-lived access token carrying the user id as the "subject".
    fun issueAccessToken(userId: Long): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(userId.toString())                                    // "sub" — who it's for
            .issuedAt(Date.from(now))                                      // "iat"
            .expiration(Date.from(now.plusSeconds(accessTtlMinutes * 60))) // "exp"
            .signWith(key)                                                 // HS256 signature
            .compact()                                                     // -> header.payload.signature
    }

    // Verify a token and return its user id, or null if it's missing/expired/tampered.
    // parseSignedClaims re-checks the signature AND the expiry; any failure throws,
    // and we treat every failure the same way: this token is not trustworthy.
    fun parseUserId(token: String): Long? =
        try {
            val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
            claims.subject.toLong()
        } catch (ex: JwtException) {
            null
        } catch (ex: IllegalArgumentException) {
            null
        }
}
