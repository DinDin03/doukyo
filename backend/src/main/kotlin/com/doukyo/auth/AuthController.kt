package com.doukyo.auth

import com.doukyo.common.UnauthorizedException
import com.doukyo.user.User
import com.doukyo.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller

@Controller
class AuthController(
    private val authService: AuthService,
    private val userRepository: UserRepository,
) {
    @MutationMapping
    fun signUp(@Argument name: String, @Argument email: String, @Argument password: String): AuthPayload =
        authService.signUp(name, email, password)

    @MutationMapping
    fun signIn(@Argument email: String, @Argument password: String): AuthPayload =
        authService.signIn(email, password)

    @MutationMapping
    fun refresh(@Argument refreshToken: String): AuthPayload =
        authService.refresh(refreshToken)

    // The signed-in user. The principal was set by JwtAuthFilter from the Bearer
    // token; if there's none (anonymous), principal isn't a Long -> not signed in.
    @QueryMapping
    fun me(): User {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        val userId = principal as? Long ?: throw UnauthorizedException("You are not signed in")
        return userRepository.findByIdOrNull(userId)
            ?: throw UnauthorizedException("This account no longer exists")
    }
}
