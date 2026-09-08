package com.doukyo.auth

import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper

// Sends through whatever SMTP server is configured. Every provider speaks SMTP,
// so moving from Gmail to Resend or Postmark is a change of credentials, not code.
class SmtpEmailSender(
    private val mailSender: JavaMailSender,
    private val from: String,
) : EmailSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendSignUpCode(email: String, code: String) {
        send(
            to = email,
            subject = "$code is your Doukyo code",
            // Plain text matters: some clients show it, and a mail with no text
            // part scores worse with spam filters.
            text = """
                Your Doukyo verification code is $code

                It expires in 10 minutes and can only be used once.
                If you didn't try to create an account, you can ignore this email.
            """.trimIndent(),
            html = codeHtml(code),
        )
    }

    override fun sendSignUpAttemptOnExistingAccount(email: String) {
        send(
            to = email,
            subject = "Someone tried to sign up with your email",
            text = """
                Someone just tried to create a Doukyo account with this address,
                but you already have one.

                If that was you, sign in instead — or reset your password if you've
                forgotten it. If it wasn't, you can safely ignore this: no account
                was created and nothing has changed.
            """.trimIndent(),
            html = existingAccountHtml(),
        )
    }

    // Failures are logged, never thrown. The caller already returned "if that
    // address can receive mail, a code is on the way" — turning a provider outage
    // into a user-visible error would also leak that the address was accepted.
    private fun send(to: String, subject: String, text: String, html: String) {
        try {
            val message: MimeMessage = mailSender.createMimeMessage()
            // multipart = true so the client can pick html or text.
            MimeMessageHelper(message, true, "UTF-8").apply {
                setFrom(from)
                setTo(to)
                setSubject(subject)
                setText(text, html)
            }
            mailSender.send(message)
            log.info("Sent '{}' to {}", subject, to)
        } catch (e: Exception) {
            log.error("Failed to send '{}' to {}", subject, to, e)
        }
    }

    // Inline styles only, and a table-free layout: mail clients strip <style>
    // blocks and support CSS about as well as a browser did in 2005.
    private fun codeHtml(code: String) = wrapper(
        """
        <p style="margin:0 0 18px;font-size:15px;line-height:1.6;color:#4a423d;">
          Enter this code in the app to finish creating your account.
        </p>
        <div style="margin:24px 0;padding:18px;border:1px solid #e2dad5;border-radius:12px;text-align:center;">
          <div style="font-size:34px;letter-spacing:10px;color:#201f1d;font-family:Georgia,serif;">$code</div>
        </div>
        <p style="margin:0;font-size:13px;line-height:1.6;color:#8c807a;">
          It expires in 10 minutes and can only be used once.
          If you didn't try to create an account, you can ignore this email.
        </p>
        """.trimIndent(),
    )

    private fun existingAccountHtml() = wrapper(
        """
        <p style="margin:0 0 14px;font-size:15px;line-height:1.6;color:#4a423d;">
          Someone just tried to create a Doukyo account with this address, but you
          already have one.
        </p>
        <p style="margin:0;font-size:13px;line-height:1.6;color:#8c807a;">
          If that was you, sign in instead. If it wasn't, you can safely ignore
          this — no account was created and nothing has changed.
        </p>
        """.trimIndent(),
    )

    private fun wrapper(body: String) = """
        <div style="background:#f3f2f2;padding:32px 16px;font-family:-apple-system,Segoe UI,Helvetica,Arial,sans-serif;">
          <div style="max-width:460px;margin:0 auto;background:#fffcfa;border:1px solid #e2dad5;border-radius:14px;padding:30px;">
            <div style="font-family:Georgia,serif;font-size:26px;color:#201f1d;">Doukyo</div>
            <div style="font-size:12px;letter-spacing:5px;color:#b68235;margin-top:2px;">同居</div>
            <div style="height:1px;background:#e2dad5;margin:20px 0;"></div>
            $body
          </div>
        </div>
    """.trimIndent()
}
