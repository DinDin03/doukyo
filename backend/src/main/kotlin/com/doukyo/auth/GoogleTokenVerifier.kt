package com.doukyo.auth

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

data class GoogleIdentity(
    val subject: String, // Google's stable per-account id ("sub") — our provider_user_id
    val email: String,
    val emailVerified: Boolean, // Google's own assertion, not something we can fake locally
    val name: String,
)

@Component
class GoogleTokenVerifier(
    @Value("\${doukyo.security.google-client-id}") private val googleClientId: String,
) {
    private val verifier =
        GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
            .setAudience(listOf(googleClientId))
            .build()

    fun verify(idTokenString: String): GoogleIdentity {
        val idToken = try {
            verifier.verify(idTokenString)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid Google sign-in")
        } ?: throw IllegalArgumentException("Invalid Google sign-in")
        val payload = idToken.payload
        val email = payload.email
            ?: throw IllegalArgumentException("Google account has no email")
        return GoogleIdentity(
            subject = payload.subject,
            email = email,
            emailVerified = payload.emailVerified ?: false,
            name = (payload["name"] as? String) ?: email.substringBefore('@'),
        )
    }
}
