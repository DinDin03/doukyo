package com.doukyo.common

import org.springframework.security.core.context.SecurityContextHolder

// The authenticated caller's user id, read from what JwtAuthFilter already put in
// the SecurityContext. Throws UnauthorizedException if nobody's signed in.
object CurrentUser {
    fun id(): Long =
        SecurityContextHolder.getContext().authentication?.principal as? Long
            ?: throw UnauthorizedException("You are not signed in")
}
