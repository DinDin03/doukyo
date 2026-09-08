package com.doukyo.auth

import org.slf4j.LoggerFactory

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

// Fallback when no SMTP server is configured: prints the code to the log so the
// flow is usable in development without a mail account.
//
// This MUST NOT reach production — it is a "log every verification code" feature.
// EmailConfig picks it only when mail is unconfigured, and logs a warning saying so.
class LoggingEmailSender : EmailSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendSignUpCode(email: String, code: String) {
        log.warn("[DEV EMAIL] sign-up code for {} is {}", email, code)
    }

    override fun sendSignUpAttemptOnExistingAccount(email: String) {
        log.warn("[DEV EMAIL] sign-up attempted on existing account {}", email)
    }
}
