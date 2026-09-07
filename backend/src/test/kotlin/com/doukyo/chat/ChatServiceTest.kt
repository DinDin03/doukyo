package com.doukyo.chat

import com.doukyo.AbstractIntegrationTest
import com.doukyo.common.UnauthorizedException
import com.doukyo.household.Household
import com.doukyo.household.HouseholdService
import com.doukyo.user.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ChatServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var chatService: ChatService
    @Autowired private lateinit var householdService: HouseholdService
    @Autowired private lateinit var messageRepository: MessageRepository

    private lateinit var alice: User
    private lateinit var bob: User
    private lateinit var outsider: User
    private lateinit var household: Household
    private val hid: Long get() = household.id!!

    @BeforeEach
    fun setUpHousehold() {
        alice = newUser("Alice")
        bob = newUser("Bob")
        outsider = newUser("Outsider")
        household = householdService.createHousehold("Flat 7", alice.id!!)
        householdService.joinHousehold(household.inviteCode, bob.id!!)
    }

    private fun send(sender: User, body: String, clientId: String = "c-${System.nanoTime()}") =
        chatService.send(hid, sender.id!!, body, clientId)

    // --- send ---

    @Test
    fun `send persists the message and returns it resolved`() {
        val message = send(alice, "whose turn for bins?")

        assertThat(message.body).isEqualTo("whose turn for bins?")
        assertThat(message.sender.name).isEqualTo("Alice")
        assertThat(message.householdId).isEqualTo(hid)
        assertThat(message.createdAt).isNotBlank()
        assertThat(messageRepository.findAll()).hasSize(1)
    }

    @Test
    fun `send trims the body`() {
        assertThat(send(alice, "   hello   ").body).isEqualTo("hello")
    }

    @Test
    fun `send rejects an empty or whitespace-only body`() {
        listOf("", "   ", "\n\t").forEach {
            assertThatThrownBy { send(alice, it) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Can't send an empty message")
        }
        assertThat(messageRepository.findAll()).isEmpty()
    }

    @Test
    fun `send rejects a body over the limit but accepts one at it`() {
        assertThat(send(alice, "x".repeat(ChatService.MAX_BODY)).body).hasSize(ChatService.MAX_BODY)

        assertThatThrownBy { send(alice, "x".repeat(ChatService.MAX_BODY + 1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("That message is too long")
    }

    @Test
    fun `send rejects a blank or over-long clientId`() {
        assertThatThrownBy { send(alice, "hi", clientId = "  ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Invalid message id")
        assertThatThrownBy { send(alice, "hi", clientId = "x".repeat(65)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `send is idempotent - the same clientId returns the original message`() {
        val first = send(alice, "only once", clientId = "dup-1")
        // A flaky network makes the app resend; this must not post twice.
        val retry = chatService.send(hid, alice.id!!, "only once", "dup-1")

        assertThat(retry.id).isEqualTo(first.id)
        assertThat(messageRepository.findAll()).hasSize(1)
    }

    @Test
    fun `the same clientId in a different household is a different message`() {
        val other = householdService.createHousehold("Other", alice.id!!)
        send(alice, "here", clientId = "shared")
        chatService.send(other.id!!, alice.id!!, "there", "shared")

        assertThat(messageRepository.findAll()).hasSize(2)
    }

    @Test
    fun `a non-member cannot send`() {
        assertThatThrownBy { chatService.send(hid, outsider.id!!, "let me in", "x") }
            .isInstanceOf(UnauthorizedException::class.java)
            .hasMessage("You're not a member of this household")
        assertThat(messageRepository.findAll()).isEmpty()
    }

    // --- history ---

    @Test
    fun `history is newest first`() {
        send(alice, "first")
        send(bob, "second")
        send(alice, "third")

        assertThat(chatService.history(hid, alice.id!!, null, null).map { it.body })
            .containsExactly("third", "second", "first")
    }

    @Test
    fun `history is empty for a household with no messages`() {
        val empty = householdService.createHousehold("Quiet", alice.id!!)
        assertThat(chatService.history(empty.id!!, alice.id!!, null, null)).isEmpty()
    }

    @Test
    fun `history honours the limit`() {
        repeat(5) { send(alice, "m$it") }
        assertThat(chatService.history(hid, alice.id!!, null, 2)).hasSize(2)
    }

    @Test
    fun `history clamps an absurd limit to the maximum`() {
        repeat(3) { send(alice, "m$it") }
        // Must not let a client request an unbounded page.
        assertThat(chatService.history(hid, alice.id!!, null, 100_000)).hasSize(3)
        assertThat(chatService.history(hid, alice.id!!, null, 0)).hasSize(1)
        assertThat(chatService.history(hid, alice.id!!, null, -5)).hasSize(1)
    }

    @Test
    fun `history pages backwards from a cursor`() {
        val ids = (1..5).map { send(alice, "m$it").id }

        // Keyset pagination: everything strictly older than the cursor.
        val older = chatService.history(hid, alice.id!!, ids[2], null)
        assertThat(older.map { it.body }).containsExactly("m2", "m1")
    }

    @Test
    fun `history only returns this household's messages`() {
        val other = householdService.createHousehold("Other", alice.id!!)
        send(alice, "mine")
        chatService.send(other.id!!, alice.id!!, "theirs", "o-1")

        assertThat(chatService.history(hid, alice.id!!, null, null).map { it.body })
            .containsExactly("mine")
    }

    @Test
    fun `a non-member cannot read history`() {
        send(alice, "private")
        assertThatThrownBy { chatService.history(hid, outsider.id!!, null, null) }
            .isInstanceOf(UnauthorizedException::class.java)
    }

    // --- messagesSince (reconnect backfill) ---

    @Test
    fun `since returns everything after the cursor, oldest first`() {
        val ids = (1..4).map { send(alice, "m$it").id }

        assertThat(chatService.since(hid, alice.id!!, ids[1]).map { it.body })
            .containsExactly("m3", "m4")
    }

    @Test
    fun `since returns nothing when the client is already up to date`() {
        val last = send(alice, "latest").id
        assertThat(chatService.since(hid, alice.id!!, last)).isEmpty()
    }

    @Test
    fun `since from zero returns the whole thread`() {
        repeat(3) { send(alice, "m$it") }
        assertThat(chatService.since(hid, alice.id!!, 0)).hasSize(3)
    }

    @Test
    fun `a non-member cannot backfill`() {
        assertThatThrownBy { chatService.since(hid, outsider.id!!, 0) }
            .isInstanceOf(UnauthorizedException::class.java)
    }

    // --- unread + markRead ---

    @Test
    fun `everything is unread until the thread is read`() {
        repeat(3) { send(alice, "m$it") }
        assertThat(chatService.unreadCount(hid, bob.id!!)).isEqualTo(3)
    }

    @Test
    fun `unread is zero for an empty thread`() {
        assertThat(chatService.unreadCount(hid, bob.id!!)).isZero()
    }

    @Test
    fun `marking read clears the count, and new messages raise it again`() {
        repeat(3) { send(alice, "m$it") }
        val latest = chatService.history(hid, bob.id!!, null, 1).first().id

        chatService.markRead(hid, bob.id!!, latest)
        assertThat(chatService.unreadCount(hid, bob.id!!)).isZero()

        send(alice, "after")
        assertThat(chatService.unreadCount(hid, bob.id!!)).isEqualTo(1)
    }

    @Test
    fun `the read bookmark never moves backwards`() {
        val ids = (1..3).map { send(alice, "m$it").id }

        chatService.markRead(hid, bob.id!!, ids[2])
        val afterRewind = chatService.markRead(hid, bob.id!!, ids[0])

        // An out-of-order call must not resurrect messages already seen.
        assertThat(afterRewind).isEqualTo(ids[2])
        assertThat(chatService.unreadCount(hid, bob.id!!)).isZero()
    }

    @Test
    fun `read bookmarks are per user`() {
        repeat(2) { send(alice, "m$it") }
        val latest = chatService.history(hid, alice.id!!, null, 1).first().id

        chatService.markRead(hid, alice.id!!, latest)

        assertThat(chatService.unreadCount(hid, alice.id!!)).isZero()
        assertThat(chatService.unreadCount(hid, bob.id!!)).isEqualTo(2)
    }

    @Test
    fun `a non-member has no unread count and cannot mark read`() {
        assertThatThrownBy { chatService.unreadCount(hid, outsider.id!!) }
            .isInstanceOf(UnauthorizedException::class.java)
        assertThatThrownBy { chatService.markRead(hid, outsider.id!!, 1) }
            .isInstanceOf(UnauthorizedException::class.java)
    }

    // --- membership checks ---

    @Test
    fun `isMember reflects household membership`() {
        assertThat(chatService.isMember(hid, alice.id!!)).isTrue()
        assertThat(chatService.isMember(hid, bob.id!!)).isTrue()
        assertThat(chatService.isMember(hid, outsider.id!!)).isFalse()
    }

    @Test
    fun `requireMember throws for a non-member only`() {
        chatService.requireMember(hid, alice.id!!)
        assertThatThrownBy { chatService.requireMember(hid, outsider.id!!) }
            .isInstanceOf(UnauthorizedException::class.java)
    }
}
