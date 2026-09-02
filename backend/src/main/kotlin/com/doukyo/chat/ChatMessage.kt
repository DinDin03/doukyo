package com.doukyo.chat

data class MessageAuthor(val id: Long, val name: String)

// What the API returns and what travels over the subscription. Fully resolved on
// purpose: subscribers are served outside any transaction, so nothing here may be
// a lazy proxy. Field names match the GraphQL `Message` type.
data class ChatMessage(
    val id: Long,
    val householdId: Long,
    val body: String,
    val clientId: String,
    val createdAt: String,
    val sender: MessageAuthor,
) {
    companion object {
        fun from(m: Message) = ChatMessage(
            id = m.id!!,
            householdId = m.household.id!!,
            body = m.body,
            clientId = m.clientId,
            createdAt = m.createdAt.toString(),
            sender = MessageAuthor(id = m.sender.id!!, name = m.sender.name),
        )
    }
}
