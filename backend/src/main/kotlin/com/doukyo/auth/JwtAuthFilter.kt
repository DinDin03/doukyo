package com.doukyo.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

// Runs once per request (OncePerRequestFilter). If there's a valid
// "Authorization: Bearer <jwt>" header, it records the authenticated user id in the
// SecurityContext for this request only. No header / invalid token => the request
// continues unauthenticated, and resolvers that require a user will reject it.
@Component
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val userId = jwtService.parseUserId(header.removePrefix("Bearer ").trim())
            if (userId != null) {
                // principal = the user id. No credentials, no authorities (roles) yet.
                val authentication = UsernamePasswordAuthenticationToken(userId, null, emptyList())
                SecurityContextHolder.getContext().authentication = authentication
            }
        }
        chain.doFilter(request, response)
    }
}
