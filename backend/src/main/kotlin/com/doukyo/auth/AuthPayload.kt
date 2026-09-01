package com.doukyo.auth

import com.doukyo.user.User

// What every auth mutation returns: the two tokens plus the user. Maps to the
// GraphQL `AuthPayload` type by field name.
data class AuthPayload(
    val accessToken: String,
    val refreshToken: String,
    val user: User,
)
