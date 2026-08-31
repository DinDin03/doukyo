package com.doukyo.user

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

// The GraphQL layer for users. It only translates GraphQL <-> Kotlin and delegates
// all real work to the service. Returning `User` entities directly works because
// Spring for GraphQL maps their properties (id, name, email) onto the `User` type's
// fields by name.
@Controller
class UserController(private val userService: UserService) {

    // Binds the `users` field on the Query type.
    @QueryMapping
    fun users(): List<User> = userService.findAll()

    // Binds the `createUser` field on the Mutation type.
    // @Argument pulls each named GraphQL argument into a parameter.
    @MutationMapping
    fun createUser(@Argument name: String, @Argument email: String): User =
        userService.createUser(name, email)
}
