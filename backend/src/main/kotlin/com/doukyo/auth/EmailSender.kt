package com.doukyo.auth

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

// The seam between the sign-up flow and however mail actually goes out. An
// interface so the whole flow is buildable and testable before any provider
// account exists.
interface EmailSender {

    fun sendSignUpCode(email: String, code: String)

    // Sent instead of a code when the address already has an account. The API
    // response is identical either way, so the difference lives here — where only
    // the real owner of the inbox can see it.
    fun sendSignUpAttemptOnExistingAccount(email: String)
}

// Development default: prints the code to the log. A real provider replaces it by
// being declared @Primary (which is how the tests substitute a recording fake).
//
// NOT @ConditionalOnMissingBean: that annotation is for @Bean methods in
// auto-configuration, evaluated after user beans register. On a scanned
// @Component the ordering is undefined, and it silently skipped registration —
// the app failed to start with "No qualifying bean of type EmailSender", which
// the tests could not catch because they inject their own @Primary double.
//
// This MUST NOT reach production, or it becomes a "log every code" feature.
@Component
class LoggingEmailSender : EmailSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendSignUpCode(email: String, code: String) {
        log.warn("[DEV EMAIL] sign-up code for {} is {}", email, code)
    }

    override fun sendSignUpAttemptOnExistingAccount(email: String) {
        log.warn("[DEV EMAIL] sign-up attempted on existing account {}", email)
    }
}
