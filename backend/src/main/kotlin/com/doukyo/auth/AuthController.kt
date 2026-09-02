package com.doukyo.auth

import com.doukyo.common.CurrentUser
import com.doukyo.common.UnauthorizedException
import com.doukyo.user.User
import com.doukyo.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
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

    @MutationMapping
    fun googleSignIn(@Argument idToken: String): AuthPayload =
        authService.googleSignIn(idToken)

    @MutationMapping
    fun signOut(@Argument refreshToken: String): Boolean {
        authService.signOut(refreshToken)
        return true
    }

    @QueryMapping
    fun me(): User =
        userRepository.findByIdOrNull(CurrentUser.id())
            ?: throw UnauthorizedException("This account no longer exists")
}
