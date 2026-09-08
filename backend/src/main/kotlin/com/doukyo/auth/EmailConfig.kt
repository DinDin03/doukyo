package com.doukyo.auth

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender

@Configuration
class EmailConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    // Chosen explicitly rather than with @ConditionalOn* annotations. Those are
    // evaluated during scanning with undefined ordering outside auto-configuration,
    // and @ConditionalOnMissingBean on a @Component already cost us a startup
    // failure here. A plain if/else is impossible to get subtly wrong.
    //
    // Spring Boot only creates a JavaMailSender when spring.mail.host is set, so
    // ObjectProvider lets us ask "is mail configured?" without requiring it.
    @Bean
    fun configuredEmailSender(
        mailSender: ObjectProvider<JavaMailSender>,
        @Value("\${doukyo.mail.from:}") from: String,
    ): EmailSender {
        val sender = mailSender.ifAvailable
        if (sender != null && from.isNotBlank()) {
            log.info("Email: sending over SMTP as {}", from)
            return SmtpEmailSender(sender, from)
        }
        log.warn(
            "Email: NO SMTP configured — verification codes will be written to this log. " +
                "Set SPRING_MAIL_HOST, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD and DOUKYO_MAIL_FROM to send real mail.",
        )
        return LoggingEmailSender()
    }
}
