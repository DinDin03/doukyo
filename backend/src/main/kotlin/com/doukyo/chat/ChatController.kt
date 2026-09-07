package com.doukyo.chat

import com.doukyo.common.CurrentUser
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.ContextValue
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

@Controller
class ChatController(
    private val chatService: ChatService,
    private val publisher: MessagePublisher,
) {
    @QueryMapping
    fun messages(
        @Argument householdId: Long,
        @Argument before: Long?,
        @Argument limit: Int?,
    ): List<ChatMessage> = chatService.history(householdId, CurrentUser.id(), before, limit)

    @QueryMapping
    fun messagesSince(@Argument householdId: Long, @Argument after: Long): List<ChatMessage> =
        chatService.since(householdId, CurrentUser.id(), after)

    @QueryMapping
    fun unreadMessageCount(@Argument householdId: Long): Long =
        chatService.unreadCount(householdId, CurrentUser.id())

    @MutationMapping
    fun sendMessage(
        @Argument householdId: Long,
        @Argument body: String,
        @Argument clientId: String,
    ): ChatMessage = chatService.send(householdId, CurrentUser.id(), body, clientId)

    @MutationMapping
    fun markMessagesRead(@Argument householdId: Long, @Argument messageId: Long): Long =
        chatService.markRead(householdId, CurrentUser.id(), messageId)

    // Subscriptions arrive over WebSocket, which never passes through the servlet
    // security filter — so the user id comes from the GraphQL context that
    // WebSocketAuthInterceptor populated at connection_init, not SecurityContextHolder.
    @SubscriptionMapping
    fun messageAdded(
        @Argument householdId: Long,
        @ContextValue(name = WS_USER_ID, required = false) userId: Long?,
    ): Flux<ChatMessage> {
        val caller = userId ?: return Flux.error(IllegalArgumentException("Not signed in"))
        chatService.requireMember(householdId, caller) // fail fast at subscribe time
        return publisher.messagesOf(householdId)
            // The resolver body runs ONCE, so the check above can't see a membership
            // revoked later. Re-checking per emission ends the stream instead of
            // delivering to someone who has since left the household.
            // boundedElastic: isMember() blocks on JDBC, and the sink emits on the
            // publisher's thread — without this, one slow check stalls every subscriber.
            .publishOn(Schedulers.boundedElastic())
            .takeWhile { chatService.isMember(householdId, caller) }
    }
}
