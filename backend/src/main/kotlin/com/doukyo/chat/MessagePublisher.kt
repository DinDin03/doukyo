package com.doukyo.chat

import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

// In-process fan-out to every open subscription. Correct for a single backend
// instance; the day a second one exists, subscribers on A stop seeing publishes
// from B and this needs Redis Pub/Sub behind it (design doc, D7).
@Component
class MessagePublisher {

    private val sink = Sinks.many().multicast().onBackpressureBuffer<ChatMessage>(256, false)

    fun publish(message: ChatMessage) {
        sink.tryEmitNext(message)
    }

    fun messagesOf(householdId: Long): Flux<ChatMessage> =
        sink.asFlux().filter { it.householdId == householdId }
}
