package com.doukyo.household

import com.doukyo.AbstractIntegrationTest
import com.doukyo.common.ForbiddenException
import com.doukyo.expense.ExpenseCategory
import com.doukyo.expense.ExpenseRepository
import com.doukyo.expense.ExpenseService
import com.doukyo.expense.ParticipantInput
import com.doukyo.expense.SplitMethod
import com.doukyo.user.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class LeaveHouseholdTest : AbstractIntegrationTest() {

    @Autowired private lateinit var householdService: HouseholdService
    @Autowired private lateinit var householdRepository: HouseholdRepository
    @Autowired private lateinit var expenseService: ExpenseService
    @Autowired private lateinit var expenseRepository: ExpenseRepository
    @Autowired private lateinit var jdbc: JdbcTemplate
    @Autowired private lateinit var tx: TransactionTemplate

    private lateinit var alice: User
    private lateinit var bob: User
    private lateinit var carol: User
    private lateinit var household: Household
    private val hid: Long get() = household.id!!

    @BeforeEach
    fun setUpHousehold() {
        alice = newUser("Alice")
        bob = newUser("Bob")
        carol = newUser("Carol")
        household = householdService.createHousehold("Flat 7", alice.id!!)
        householdService.joinHousehold(household.inviteCode, bob.id!!)
        householdService.joinHousehold(household.inviteCode, carol.id!!)
    }

    private fun owes(ower: User, payer: User, cents: Long = 5_000) =
        expenseService.createExpense(
            householdId = hid,
            callerId = payer.id!!,
            paidById = payer.id!!,
            description = "Groceries",
            amountCents = cents,
            category = ExpenseCategory.GROCERIES,
            method = SplitMethod.EXACT,
            participants = listOf(ParticipantInput(ower.id!!, cents)),
        )

    private fun leave(user: User, confirmDelete: Boolean = false) =
        householdService.leaveHousehold(hid, user.id!!, confirmDelete)

    private fun memberNames() = householdService.findMembers(hid).map { it.name }
    private fun reload() = householdRepository.findByIdOrNull(hid)!!

    // ---------------------------------------------------------------
    // Leaving
    // ---------------------------------------------------------------

    @Test
    fun `leaving removes you and leaves the household to everyone else`() {
        leave(carol)

        assertThat(memberNames()).containsExactlyInAnyOrder("Alice", "Bob")
        assertThat(householdService.findByIdForMember(hid, carol.id!!)).isNull()
        assertThat(householdService.findMyHouseholds(carol.id!!)).isEmpty()
        assertThat(reload().deletedAt).isNull()
    }

    @Test
    fun `leaving rotates the invite code so the old one stops working`() {
        val oldCode = household.inviteCode
        leave(carol)

        assertThat(reload().inviteCode).isNotEqualTo(oldCode)
        // The hole rotation closes: without it, leaving is undone by rejoining.
        assertThatThrownBy { householdService.joinHousehold(oldCode, carol.id!!) }
            .hasMessage("That invite code doesn't match a household")
    }

    @Test
    fun `you cannot leave while you owe a housemate`() {
        owes(carol, alice)

        assertThatThrownBy { leave(carol) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Settle up")
        assertThat(memberNames()).contains("Carol")
    }

    @Test
    fun `you cannot leave while a housemate owes you`() {
        owes(alice, carol)

        assertThatThrownBy { leave(carol) }.hasMessageContaining("Settle up")
    }

    @Test
    fun `a net balance of zero is not enough to leave`() {
        // Alice owes Bob 50, Carol owes Alice 50: Alice nets to zero, yet Bob is
        // still owed by her. Letting her go would strand his money.
        owes(alice, bob)
        owes(carol, alice)

        assertThatThrownBy { leave(alice) }.hasMessageContaining("Settle up")
    }

    @Test
    fun `settled debts do not block leaving`() {
        val expense = owes(carol, alice)
        expense.shares.forEach { expenseService.settleShare(it.id!!, alice.id!!) }

        leave(carol)

        assertThat(memberNames()).doesNotContain("Carol")
    }

    @Test
    fun `debts between other housemates do not block you`() {
        owes(bob, alice)

        leave(carol)

        assertThat(memberNames()).doesNotContain("Carol")
    }

    @Test
    fun `a non-member cannot leave`() {
        val outsider = newUser("Outsider")
        assertThatThrownBy { householdService.leaveHousehold(hid, outsider.id!!, false) }
            .isInstanceOf(ForbiddenException::class.java)
        assertThatThrownBy { householdService.leaveHousehold(99999L, alice.id!!, false) }
            .isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    fun `confirmDelete does nothing while others remain`() {
        leave(carol, confirmDelete = true)

        assertThat(reload().deletedAt).isNull()
        assertThat(memberNames()).containsExactlyInAnyOrder("Alice", "Bob")
    }

    // ---------------------------------------------------------------
    // The last member
    // ---------------------------------------------------------------

    @Test
    fun `the last member must confirm, and the server enforces it`() {
        leave(bob)
        leave(carol)

        assertThatThrownBy { leave(alice) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("last member")
        assertThat(memberNames()).containsExactly("Alice")
        assertThat(reload().deletedAt).isNull()
    }

    @Test
    fun `the last member leaving soft deletes the household and keeps its data`() {
        val expense = owes(bob, alice)
        expense.shares.forEach { expenseService.settleShare(it.id!!, alice.id!!) }
        leave(bob)
        leave(carol)

        leave(alice, confirmDelete = true)

        val deleted = reload()
        assertThat(deleted.deletedAt).isNotNull()
        assertThat(deleted.deletedBy).isEqualTo(alice.id)
        assertThat(householdService.findMyHouseholds(alice.id!!)).isEmpty()
        assertThat(expenseRepository.findByHouseholdWithShares(hid)).hasSize(1)
    }

    @Test
    fun `a deleted household cannot be joined, even with its current code`() {
        leave(bob)
        leave(carol)
        leave(alice, confirmDelete = true)

        assertThatThrownBy { householdService.joinHousehold(reload().inviteCode, bob.id!!) }
            .hasMessage("That invite code doesn't match a household")
    }

    // ---------------------------------------------------------------
    // Restoring
    // ---------------------------------------------------------------

    private fun deleteAsAlice() {
        leave(bob)
        leave(carol)
        leave(alice, confirmDelete = true)
    }

    private fun ageDeletion(days: Long) =
        jdbc.update("UPDATE households SET deleted_at = now() - make_interval(days => ?) WHERE id = ?", days.toInt(), hid)

    @Test
    fun `whoever deleted a household can see it to restore, and nobody else can`() {
        deleteAsAlice()

        assertThat(householdService.findRestorableHouseholds(alice.id!!).map { it.name }).containsExactly("Flat 7")
        assertThat(householdService.findRestorableHouseholds(bob.id!!)).isEmpty()
    }

    @Test
    fun `restoring brings the household back with you in it and its data intact`() {
        owes(bob, alice).shares.forEach { expenseService.settleShare(it.id!!, alice.id!!) }
        deleteAsAlice()

        val restored = householdService.restoreHousehold(hid, alice.id!!)

        assertThat(restored.deletedAt).isNull()
        assertThat(memberNames()).containsExactly("Alice")
        assertThat(householdService.findMyHouseholds(alice.id!!).map { it.name }).containsExactly("Flat 7")
        assertThat(householdService.findRestorableHouseholds(alice.id!!)).isEmpty()
        assertThat(expenseRepository.findByHouseholdWithShares(hid)).hasSize(1)
    }

    @Test
    fun `the restore window closes after 30 days`() {
        deleteAsAlice()

        ageDeletion(29)
        assertThat(householdService.findRestorableHouseholds(alice.id!!)).hasSize(1)

        ageDeletion(31)
        assertThat(householdService.findRestorableHouseholds(alice.id!!)).isEmpty()
        assertThatThrownBy { householdService.restoreHousehold(hid, alice.id!!) }
            .hasMessage("That household can't be restored")
    }

    @Test
    fun `restore refuses anyone but the deleter, and live households, with one answer`() {
        deleteAsAlice()
        // Same message for all three, so restore can't be used to probe ids.
        assertThatThrownBy { householdService.restoreHousehold(hid, bob.id!!) }
            .hasMessage("That household can't be restored")

        householdService.restoreHousehold(hid, alice.id!!)
        assertThatThrownBy { householdService.restoreHousehold(hid, alice.id!!) }
            .hasMessage("That household can't be restored")
        assertThatThrownBy { householdService.restoreHousehold(99999L, alice.id!!) }
            .hasMessage("That household can't be restored")
    }

    // ---------------------------------------------------------------
    // Races. Another transaction holds the household row lock and changes
    // things under it; the call being tested starts while the lock is held.
    // Without the lock, that call reads the pre-change state and succeeds.
    // ---------------------------------------------------------------

    private fun <T> raceAgainstLockHolder(underLock: () -> Unit, contender: () -> T): CompletableFuture<T> {
        val locked = CountDownLatch(1)
        val release = CountDownLatch(1)
        val holder = CompletableFuture.runAsync {
            tx.executeWithoutResult {
                jdbc.queryForList("SELECT id FROM households WHERE id = ? FOR UPDATE", hid)
                underLock()
                locked.countDown()
                release.await()
            }
        }
        locked.await(5, TimeUnit.SECONDS)
        val result = CompletableFuture.supplyAsync(contender)
        Thread.sleep(500)
        assertThat(result).describedAs("contender should wait for the household lock").isNotDone
        release.countDown()
        holder.get(5, TimeUnit.SECONDS)
        return result
    }

    private fun failureOf(future: CompletableFuture<*>): Throwable =
        try {
            future.get(5, TimeUnit.SECONDS)
            error("expected the contender to fail")
        } catch (e: ExecutionException) {
            e.cause!!
        }

    @Test
    fun `two last members leaving at once cannot orphan the household`() {
        leave(carol)
        // Alice is mid-leave, uncommitted, when Bob tries.
        val bobLeaving = raceAgainstLockHolder(
            underLock = { jdbc.update("DELETE FROM memberships WHERE user_id = ? AND household_id = ?", alice.id, hid) },
            contender = { leave(bob) },
        )

        assertThat(failureOf(bobLeaving)).hasMessageContaining("last member")
        assertThat(memberNames()).containsExactly("Bob")
    }

    @Test
    fun `joining cannot slip into a household as it is being deleted`() {
        val code = household.inviteCode
        val dave = newUser("Dave")
        val daveJoining = raceAgainstLockHolder(
            underLock = { jdbc.update("UPDATE households SET deleted_at = now(), invite_code = 'GONE22' WHERE id = ?", hid) },
            contender = { householdService.joinHousehold(code, dave.id!!) },
        )

        assertThat(failureOf(daveJoining)).hasMessage("That invite code doesn't match a household")
    }
}
