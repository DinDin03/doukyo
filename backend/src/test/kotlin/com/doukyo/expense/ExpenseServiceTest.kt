package com.doukyo.expense

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

class ExpenseServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var expenseService: ExpenseService
    @Autowired private lateinit var householdService: HouseholdService
    @Autowired private lateinit var expenseRepository: ExpenseRepository

    private lateinit var alice: User
    private lateinit var bob: User
    private lateinit var carol: User
    private lateinit var outsider: User
    private lateinit var household: Household
    private val hid: Long get() = household.id!!

    @BeforeEach
    fun setUpHousehold() {
        alice = newUser("Alice")
        bob = newUser("Bob")
        carol = newUser("Carol")
        outsider = newUser("Outsider")
        household = householdService.createHousehold("Flat 7", alice.id!!)
        householdService.joinHousehold(household.inviteCode, bob.id!!)
        householdService.joinHousehold(household.inviteCode, carol.id!!)
    }

    private fun everyone() = listOf(alice, bob, carol).map { ParticipantInput(it.userId()) }
    private fun User.userId() = this.id!!

    private fun addExpense(
        payer: User = alice,
        amountCents: Long = 10_000,
        method: SplitMethod = SplitMethod.EVENLY,
        participants: List<ParticipantInput> = everyone(),
        caller: User = alice,
        description: String = "Groceries",
    ) = expenseService.createExpense(
        householdId = hid,
        callerId = caller.userId(),
        paidById = payer.userId(),
        description = description,
        amountCents = amountCents,
        category = ExpenseCategory.GROCERIES,
        method = method,
        participants = participants,
    )

    private fun sharesOf(expenseId: Long) =
        expenseRepository.findByHouseholdWithShares(hid).first { it.id == expenseId }.shares

    // ---------------------------------------------------------------
    // createExpense
    // ---------------------------------------------------------------

    @Test
    fun `creating an expense stores it with one share per participant`() {
        val expense = addExpense()

        assertThat(expense.description).isEqualTo("Groceries")
        assertThat(expense.amountCents).isEqualTo(10_000)
        assertThat(sharesOf(expense.id!!)).hasSize(3)
    }

    @Test
    fun `law - shares always sum to the expense total`() {
        // The invariant the whole aggregate exists to protect.
        listOf(10_000L, 1L, 99L, 100_001L, 7L).forEach { total ->
            val expense = addExpense(amountCents = total)
            assertThat(sharesOf(expense.id!!).sumOf { it.amountCents })
                .withFailMessage("total=%d", total)
                .isEqualTo(total)
        }
    }

    @Test
    fun `an even split distributes the remainder rather than losing it`() {
        val expense = addExpense(amountCents = 10_000)
        assertThat(sharesOf(expense.id!!).map { it.amountCents })
            .containsExactlyInAnyOrder(3_334, 3_333, 3_333)
    }

    @Test
    fun `the payer's own share is already paid, everyone else's is not`() {
        val expense = addExpense(payer = alice)
        val shares = sharesOf(expense.id!!)

        assertThat(shares.first { it.user.id == alice.userId() }.isPaid).isTrue()
        assertThat(shares.filter { it.user.id != alice.userId() }).allMatch { !it.isPaid }
    }

    @Test
    fun `the payer need not be a participant`() {
        // Alice buys food that only Bob and Carol eat: she is owed the whole amount.
        val expense = addExpense(
            payer = alice,
            participants = listOf(ParticipantInput(bob.userId()), ParticipantInput(carol.userId())),
        )
        val shares = sharesOf(expense.id!!)

        assertThat(shares).hasSize(2)
        assertThat(shares.map { it.amountCents }).containsExactlyInAnyOrder(5_000, 5_000)
        assertThat(shares).allMatch { !it.isPaid }
    }

    @Test
    fun `exact amounts are stored as given`() {
        val expense = addExpense(
            method = SplitMethod.EXACT,
            participants = listOf(
                ParticipantInput(alice.userId(), 6_000),
                ParticipantInput(bob.userId(), 3_000),
                ParticipantInput(carol.userId(), 1_000),
            ),
        )
        assertThat(sharesOf(expense.id!!).associate { it.user.id to it.amountCents })
            .containsEntry(alice.userId(), 6_000L)
            .containsEntry(bob.userId(), 3_000L)
            .containsEntry(carol.userId(), 1_000L)
    }

    @Test
    fun `exact amounts that do not sum to the total are rejected`() {
        assertThatThrownBy {
            addExpense(
                method = SplitMethod.EXACT,
                participants = listOf(ParticipantInput(alice.userId(), 6_000), ParticipantInput(bob.userId(), 3_000)),
            )
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("add up")
    }

    @Test
    fun `percentages are given in basis points`() {
        val expense = addExpense(
            method = SplitMethod.PERCENTAGE,
            participants = listOf(
                ParticipantInput(alice.userId(), 5_000), // 50.00%
                ParticipantInput(bob.userId(), 2_500),   // 25.00%
                ParticipantInput(carol.userId(), 2_500),
            ),
        )
        assertThat(sharesOf(expense.id!!).associate { it.user.id to it.amountCents })
            .containsEntry(alice.userId(), 5_000L)
            .containsEntry(bob.userId(), 2_500L)
    }

    @Test
    fun `percentages that miss one hundred percent are rejected`() {
        assertThatThrownBy {
            addExpense(
                method = SplitMethod.PERCENTAGE,
                participants = listOf(ParticipantInput(alice.userId(), 5_000), ParticipantInput(bob.userId(), 2_500)),
            )
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("100%")
    }

    @Test
    fun `weights divide proportionally`() {
        val expense = addExpense(
            amountCents = 10_000,
            method = SplitMethod.WEIGHTS,
            participants = listOf(
                ParticipantInput(alice.userId(), 2),
                ParticipantInput(bob.userId(), 1),
                ParticipantInput(carol.userId(), 1),
            ),
        )
        assertThat(sharesOf(expense.id!!).associate { it.user.id to it.amountCents })
            .containsEntry(alice.userId(), 5_000L)
            .containsEntry(bob.userId(), 2_500L)
    }

    @Test
    fun `the split is independent of the order participants are sent in`() {
        // Determinism must not depend on client ordering, or the spare cent moves
        // when the UI happens to reorder people.
        val forward = addExpense(participants = listOf(alice, bob, carol).map { ParticipantInput(it.userId()) })
        val reversed = addExpense(participants = listOf(carol, bob, alice).map { ParticipantInput(it.userId()) })

        assertThat(sharesOf(forward.id!!).associate { it.user.id to it.amountCents })
            .isEqualTo(sharesOf(reversed.id!!).associate { it.user.id to it.amountCents })
    }

    // --- authorisation ---

    @Test
    fun `a non-member cannot add an expense to the household`() {
        assertThatThrownBy { addExpense(caller = outsider) }
            .isInstanceOf(UnauthorizedException::class.java)
        assertThat(expenseRepository.findAll()).isEmpty()
    }

    @Test
    fun `the payer must be a household member`() {
        assertThatThrownBy { addExpense(payer = outsider) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("not a member")
        assertThat(expenseRepository.findAll()).isEmpty()
    }

    @Test
    fun `every participant must be a household member`() {
        // Otherwise a member could invent a debt for a stranger.
        assertThatThrownBy {
            addExpense(participants = listOf(ParticipantInput(alice.userId()), ParticipantInput(outsider.userId())))
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("not a member")
        assertThat(expenseRepository.findAll()).isEmpty()
    }

    // --- validation + atomicity ---

    @Test
    fun `a blank description is rejected`() {
        assertThatThrownBy { addExpense(description = "   ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `a non-positive amount is rejected`() {
        listOf(0L, -1L).forEach {
            assertThatThrownBy { addExpense(amountCents = it) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `an empty participant list is rejected`() {
        assertThatThrownBy { addExpense(participants = emptyList()) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `the same participant twice is rejected`() {
        assertThatThrownBy {
            addExpense(participants = listOf(ParticipantInput(alice.userId()), ParticipantInput(alice.userId())))
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("once")
    }

    @Test
    fun `validation runs before anything is written`() {
        // Note what this does NOT prove: createExpense builds the whole aggregate in
        // memory and makes a single save(), so every validation throws before any
        // write. It passes with @Transactional removed — atomicity here comes from
        // the shape of the method, and the annotation is a second line of defence
        // for the read-then-write window, not what this test exercises.
        assertThatThrownBy {
            addExpense(
                method = SplitMethod.EXACT,
                participants = listOf(ParticipantInput(alice.userId(), 1)),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)

        assertThat(expenseRepository.findAll()).isEmpty()
    }

    // ---------------------------------------------------------------
    // listing
    // ---------------------------------------------------------------

    @Test
    fun `expenses are listed newest first and scoped to the household`() {
        val other = householdService.createHousehold("Other", alice.userId())
        addExpense(description = "First")
        addExpense(description = "Second")
        expenseService.createExpense(
            other.id!!, alice.userId(), alice.userId(), "Elsewhere", 500,
            ExpenseCategory.OTHER, SplitMethod.EVENLY, listOf(ParticipantInput(alice.userId())),
        )

        assertThat(expenseService.listExpenses(hid, alice.userId()).map { it.description })
            .containsExactly("Second", "First")
    }

    @Test
    fun `a non-member cannot list expenses`() {
        addExpense()
        assertThatThrownBy { expenseService.listExpenses(hid, outsider.userId()) }
            .isInstanceOf(UnauthorizedException::class.java)
    }

    // ---------------------------------------------------------------
    // balances
    // ---------------------------------------------------------------

    @Test
    fun `law - balances always sum to zero`() {
        // Money only moves between people; it is never created or destroyed.
        addExpense(payer = alice, amountCents = 10_000)
        addExpense(payer = bob, amountCents = 3_333)
        addExpense(payer = carol, amountCents = 77, participants = listOf(ParticipantInput(alice.userId())))

        assertThat(expenseService.balances(hid, alice.userId()).sumOf { it.netCents }).isZero()
    }

    @Test
    fun `the payer is owed what the others have not paid`() {
        // Alice pays 100 split evenly with Bob: Bob owes her 50.
        addExpense(
            payer = alice,
            amountCents = 10_000,
            participants = listOf(ParticipantInput(alice.userId()), ParticipantInput(bob.userId())),
        )
        val balances = expenseService.balances(hid, alice.userId()).associate { it.userId to it.netCents }

        assertThat(balances[alice.userId()]).isEqualTo(5_000)
        assertThat(balances[bob.userId()]).isEqualTo(-5_000)
    }

    @Test
    fun `a household with no expenses has everyone at zero`() {
        assertThat(expenseService.balances(hid, alice.userId())).allMatch { it.netCents == 0L }
    }

    @Test
    fun `balances carry the member's name`() {
        addExpense()
        assertThat(expenseService.balances(hid, alice.userId()).map { it.userName })
            .containsExactlyInAnyOrder("Alice", "Bob", "Carol")
    }

    @Test
    fun `a non-member cannot read balances`() {
        assertThatThrownBy { expenseService.balances(hid, outsider.userId()) }
            .isInstanceOf(UnauthorizedException::class.java)
    }

    // ---------------------------------------------------------------
    // settling
    // ---------------------------------------------------------------

    @Test
    fun `settling a share marks it paid and clears that debt`() {
        val expense = addExpense(
            payer = alice,
            amountCents = 10_000,
            participants = listOf(ParticipantInput(alice.userId()), ParticipantInput(bob.userId())),
        )
        val bobsShare = sharesOf(expense.id!!).first { it.user.id == bob.userId() }

        expenseService.settleShare(bobsShare.id!!, bob.userId())

        val balances = expenseService.balances(hid, alice.userId()).associate { it.userId to it.netCents }
        assertThat(balances[alice.userId()]).isZero()
        assertThat(balances[bob.userId()]).isZero()
    }

    @Test
    fun `settling never deletes the share - the ledger is kept`() {
        val expense = addExpense()
        val share = sharesOf(expense.id!!).first { it.user.id == bob.userId() }

        expenseService.settleShare(share.id!!, bob.userId())

        assertThat(sharesOf(expense.id!!)).hasSize(3)
        assertThat(sharesOf(expense.id!!).first { it.user.id == bob.userId() }.isPaid).isTrue()
    }

    @Test
    fun `settling twice is harmless`() {
        val expense = addExpense()
        val share = sharesOf(expense.id!!).first { it.user.id == bob.userId() }

        expenseService.settleShare(share.id!!, bob.userId())
        expenseService.settleShare(share.id!!, bob.userId())

        assertThat(sharesOf(expense.id!!).first { it.user.id == bob.userId() }.isPaid).isTrue()
    }

    @Test
    fun `a non-member cannot settle a share`() {
        val expense = addExpense()
        val share = sharesOf(expense.id!!).first { it.user.id == bob.userId() }

        assertThatThrownBy { expenseService.settleShare(share.id!!, outsider.userId()) }
            .isInstanceOf(UnauthorizedException::class.java)
    }

    @Test
    fun `settling an unknown share is rejected`() {
        assertThatThrownBy { expenseService.settleShare(999_999L, alice.userId()) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
