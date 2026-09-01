package com.doukyo.common

// Thrown when a request needs a signed-in user but doesn't have one, or a session
// is expired. Mapped to a GraphQL UNAUTHORIZED error by GraphQlExceptionResolver.
class UnauthorizedException(message: String) : RuntimeException(message)
