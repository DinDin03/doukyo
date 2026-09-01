package com.doukyo.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(private val jwtAuthFilter: JwtAuthFilter) {

    // The one password hasher for the whole app. BCryptPasswordEncoder(): a default
    // work factor of 10 (2^10 rounds). encode() hashes, matches() verifies.
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Token-based auth uses no cookies, so CSRF (a cookie-session attack)
            // doesn't apply — disable it, otherwise it would block our POSTs.
            .csrf { it.disable() }
            // No server-side session: every request re-authenticates from its token.
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            // GraphQL is a single endpoint; URL rules can't tell signIn from `me`.
            // Permit all at the HTTP layer and enforce auth inside the resolvers.
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            // Run our JWT filter early, so the SecurityContext is set before resolvers.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
