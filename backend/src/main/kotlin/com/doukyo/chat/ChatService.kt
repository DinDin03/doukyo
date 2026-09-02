package com.doukyo.chat

import com.doukyo.common.UnauthorizedException
import com.doukyo.household.HouseholdRepository
import com.doukyo.household.MembershipRepository
import com.doukyo.user.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class ChatService(
    private val messageRepository: MessageRepository,
    private val messageReadRepository: MessageReadRepository,
    private val membershipRepository: MembershipRepository,
    private val householdRepository: HouseholdRepository,
    private val userRepository: UserRepository,
    private val publisher: MessagePublisher,
) {
    companion object {
        const val MAX_BODY = 2000
        const val DEFAULT_PAGE = 50
        const val MAX_PAGE = 100
    }

    @Transactional
    fun send(householdId: Long, userId: Long, body: String, clientId: String): ChatMessage {
        requireMember(householdId, userId)
        val text = body.trim()
        require(text.isNotEmpty()) { "Can't send an empty message" }
        require(text.length <= MAX_BODY) { "That message is too long" }
        require(clientId.isNotBlank() && clientId.length <= 64) { "Invalid message id" }

        // Idempotent: a retry after a timeout returns the message already stored
        // rather than posting it twice.
        messageRepository.findByHouseholdIdAndClientId(householdId, clientId)?.let {
            return ChatMessage.from(it)
        }

        val household = householdRepository.findByIdOrNull(householdId)
            ?: throw IllegalArgumentException("No household with id $householdId")
        val sender = userRepository.findByIdOrNull(userId)
            ?: throw UnauthorizedException("This account no longer exists")

        val saved = messageRepository.save(
            Message(household = household, sender = sender, body = text, clientId = clientId),
        )
        val message = ChatMessage.from(saved)
        publisher.publish(message)
        return message
    }

    // Newest first — what the chat screen loads on open and pages back through.
    @Transactional(readOnly = true)
    fun history(householdId: Long, userId: Long, before: Long?, limit: Int?): List<ChatMessage> {
        requireMember(householdId, userId)
        val page = PageRequest.of(0, (limit ?: DEFAULT_PAGE).coerceIn(1, MAX_PAGE))
        val rows = if (before == null) {
            messageRepository.findLatest(householdId, page)
        } else {
            messageRepository.findBefore(householdId, before, page)
        }
        return rows.map(ChatMessage::from)
    }

    // Oldest first — the reconnect backfill that keeps the thread correct after a
    // dropped socket.
    @Transactional(readOnly = true)
    fun since(householdId: Long, userId: Long, after: Long): List<ChatMessage> {
        requireMember(householdId, userId)
        return messageRepository.findAfter(householdId, after).map(ChatMessage::from)
    }

    @Transactional(readOnly = true)
    fun unreadCount(householdId: Long, userId: Long): Long {
        requireMember(householdId, userId)
        val lastRead = messageReadRepository.findByUserIdAndHouseholdId(userId, householdId)?.lastReadMessageId ?: 0
        return messageRepository.countByHouseholdIdAndIdGreaterThan(householdId, lastRead)
    }

    @Transactional
    fun markRead(householdId: Long, userId: Long, messageId: Long): Long {
        requireMember(householdId, userId)
        val existing = messageReadRepository.findByUserIdAndHouseholdId(userId, householdId)
        if (existing != null) {
            // Never move the bookmark backwards — out-of-order calls shouldn't
            // resurrect messages the user has already seen.
            if (messageId > existing.lastReadMessageId) {
                existing.lastReadMessageId = messageId
                existing.updatedAt = OffsetDateTime.now()
            }
            return existing.lastReadMessageId
        }
        val household = householdRepository.findByIdOrNull(householdId)
            ?: throw IllegalArgumentException("No household with id $householdId")
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UnauthorizedException("This account no longer exists")
        messageReadRepository.save(
            MessageRead(user = user, household = household, lastReadMessageId = messageId),
        )
        return messageId
    }

    // The only authorisation rule in the whole feature: you must be in the house.
    // There is no separate chat roster to consult (design doc, D2).
    fun requireMember(householdId: Long, userId: Long) {
        if (!membershipRepository.existsByUserIdAndHouseholdId(userId, householdId)) {
            throw UnauthorizedException("You're not a member of this household")
        }
    }
}
