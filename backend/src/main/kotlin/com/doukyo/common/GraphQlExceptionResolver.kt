package com.doukyo.common

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

// Maps specific, SAFE exceptions to meaningful GraphQL errors. Anything not handled
// here falls through and Spring masks it as INTERNAL_ERROR — the secure default that
// prevents leaking stack traces / internals to clients.
@Component
class GraphQlExceptionResolver : DataFetcherExceptionResolverAdapter() {

    override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? =
        when (ex) {
            // `require(...)` in our services throws IllegalArgumentException for a
            // broken business rule (e.g. duplicate email). That's a client mistake,
            // so surface it as BAD_REQUEST with the real message.
            is IllegalArgumentException ->
                GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.BAD_REQUEST)
                    // Defensive: an exception's .message CAN be null (either thrown
                    // with no message, or from third-party code we don't control).
                    // Never pass null into the error builder — fall back to a safe
                    // generic message instead.
                    .message(ex.message ?: "That request wasn't valid")
                    .build()

            // Missing/expired auth -> UNAUTHORIZED with a safe message.
            is UnauthorizedException ->
                GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.UNAUTHORIZED)
                    .message(ex.message)
                    .build()

            // Not one we recognise -> return null so Spring masks it (INTERNAL_ERROR).
            else -> null
        }
}
