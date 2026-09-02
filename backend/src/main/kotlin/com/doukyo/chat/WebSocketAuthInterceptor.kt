package com.doukyo.chat

import com.doukyo.auth.JwtService
import org.springframework.graphql.server.WebGraphQlInterceptor
import org.springframework.graphql.server.WebGraphQlRequest
import org.springframework.graphql.server.WebGraphQlResponse
import org.springframework.graphql.server.WebSocketGraphQlInterceptor
import org.springframework.graphql.server.WebSocketGraphQlRequest
import org.springframework.graphql.server.WebSocketSessionInfo
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

const val WS_USER_ID = "userId"

// A browser can't set an Authorization header on a WebSocket handshake, so the
// token arrives in the graphql-ws connection_init payload instead. We authenticate
// the connection once here, then copy the user id into the GraphQL context of every
// operation on that socket.
@Component
class WebSocketAuthInterceptor(private val jwtService: JwtService) : WebSocketGraphQlInterceptor {

    override fun handleConnectionInitialization(
        sessionInfo: WebSocketSessionInfo,
        connectionInitPayload: MutableMap<String, Any>,
    ): Mono<Any> {
        val raw = (connectionInitPayload["Authorization"] ?: connectionInitPayload["authorization"]) as? String
        val token = raw?.removePrefix("Bearer ")?.trim()
        val userId = token?.takeIf { it.isNotEmpty() }?.let(jwtService::parseUserId)
            ?: return Mono.error(IllegalArgumentException("Not signed in"))
        sessionInfo.attributes[WS_USER_ID] = userId
        return Mono.empty()
    }

    override fun intercept(request: WebGraphQlRequest, chain: WebGraphQlInterceptor.Chain): Mono<WebGraphQlResponse> {
        if (request is WebSocketGraphQlRequest) {
            val userId = request.sessionInfo.attributes[WS_USER_ID]
            if (userId != null) {
                request.configureExecutionInput { _, builder ->
                    builder.graphQLContext(mapOf(WS_USER_ID to userId)).build()
                }
            }
        }
        return chain.next(request)
    }
}
