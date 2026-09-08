package com.doukyo.expense

import com.doukyo.common.UnauthorizedException
import com.doukyo.household.HouseholdRepository
import com.doukyo.household.MembershipRepository
import com.doukyo.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

enum class SplitMethod { EVENLY, EXACT, PERCENTAGE, WEIGHTS }

// `value` means whatever the method says it means: ignored for EVENLY, cents for
// EXACT, basis points for PERCENTAGE, a weight for WEIGHTS. One flat shape rather
// than a sealed hierarchy because GraphQL has no union input types — the API
// boundary would have to flatten it anyway.
data class ParticipantInput(val userId: Long, val value: Long = 0)

data class Balance(val userId: Long, val userName: String, val netCents: Long)

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val expenseShareRepository: ExpenseShareRepository,
    private val membershipRepository: MembershipRepository,
    private val householdRepository: HouseholdRepository,
    private val userRepository: UserRepository,
) {
    companion object {
        const val MAX_DESCRIPTION = 255
    }

    // Everything — validation, the split, and both tables — inside one transaction.
    // A half-written expense would break "shares sum to the total" permanently, and
    // nothing would ever detect it.
    @Transactional
    fun createExpense(
        householdId: Long,
        callerId: Long,
        paidById: Long,
        description: String,
        amountCents: Long,
        category: ExpenseCategory,
        method: SplitMethod,
        participants: List<ParticipantInput>,
    ): Expense {
        requireMember(householdId, callerId)

        val cleanDescription = description.trim()
        require(cleanDescription.isNotBlank()) { "Please describe the expense" }
        require(cleanDescription.length <= MAX_DESCRIPTION) { "That description is too long" }
        require(amountCents > 0) { "An amount must be greater than zero" }
        require(participants.isNotEmpty()) { "An expense needs at least one participant" }
        require(participants.map { it.userId }.toSet().size == participants.size) {
            "Each participant can only appear once"
        }

        // Caller-supplied ids are never trusted: the payer and every participant must
        // belong to this household, or a member could invent a debt for a stranger.
        requireHouseholdMember(householdId, paidById)
        participants.forEach { requireHouseholdMember(householdId, it.userId) }

        // Sorted so the split depends only on WHO is involved, never on the order the
        // client happened to send them — otherwise the spare cent moves when the UI
        // reorders people.
        val ordered = participants.sortedBy { it.userId }
        val amounts = splitAmounts(method, amountCents, ordered)

        val household = householdRepository.findByIdOrNull(householdId)
            ?: throw IllegalArgumentException("No household with id $householdId")
        val payer = userRepository.findByIdOrNull(paidById)
            ?: throw IllegalArgumentException("No user with id $paidById")

        val expense = Expense(
            household = household,
            paidBy = payer,
            description = cleanDescription,
            amountCents = amountCents,
            category = category,
        )
        ordered.forEachIndexed { i, participant ->
            val user = userRepository.findByIdOrNull(participant.userId)
                ?: throw IllegalArgumentException("No user with id ${participant.userId}")
            expense.shares += ExpenseShare(
                expense = expense,
                user = user,
                amountCents = amounts[i],
                // The payer already covered their own portion.
                isPaid = participant.userId == paidById,
            )
        }

        // cascade = ALL on Expense.shares writes the shares in this same transaction.
        return expenseRepository.save(expense)
    }

    @Transactional(readOnly = true)
    fun listExpenses(householdId: Long, callerId: Long): List<Expense> {
        requireMember(householdId, callerId)
        return expenseRepository.findByHouseholdWithShares(householdId)
    }

    // Derived, never stored: a balance column would have to be updated on every
    // create, edit, delete and settle, and the first missed path makes it drift.
    @Transactional(readOnly = true)
    fun balances(householdId: Long, callerId: Long): List<Balance> {
        requireMember(householdId, callerId)

        val members = membershipRepository.findMembersOfHousehold(householdId).map { it.user }
        val net = members.associate { it.id!! to 0L }.toMutableMap()

        expenseShareRepository.findUnpaidInHousehold(householdId).forEach { share ->
            val ower = share.user.id!!
            val payer = share.expense.paidBy.id!!
            // Unreachable today — the payer's own share is created already paid, and
            // this reads only unpaid shares. Kept because it is one line guarding a
            // money total: if an "unsettle" ever lands, its absence would silently
            // credit the payer for owing themselves.
            if (ower != payer) {
                net[ower] = (net[ower] ?: 0) - share.amountCents
                net[payer] = (net[payer] ?: 0) + share.amountCents
            }
        }

        return members.map { Balance(it.id!!, it.name, net[it.id!!] ?: 0) }
    }

    // Settling flips a flag; it never deletes the row. Deleting would break the
    // sum invariant and lose the record of who paid what.
    @Transactional
    fun settleShare(shareId: Long, callerId: Long): ExpenseShare {
        val share = expenseShareRepository.findByIdOrNull(shareId)
            ?: throw IllegalArgumentException("No share with id $shareId")
        requireMember(share.expense.household.id!!, callerId)
        share.isPaid = true
        return share
    }

    private fun splitAmounts(
        method: SplitMethod,
        totalCents: Long,
        participants: List<ParticipantInput>,
    ): List<Long> = when (method) {
        SplitMethod.EVENLY -> SplitCalculator.evenly(totalCents, participants.size)
        SplitMethod.WEIGHTS -> SplitCalculator.byWeights(totalCents, participants.map { it.value })
        SplitMethod.EXACT -> SplitCalculator.exact(totalCents, participants.map { it.value })
        SplitMethod.PERCENTAGE ->
            SplitCalculator.byBasisPoints(totalCents, participants.map { it.value.toInt() })
    }

    private fun requireMember(householdId: Long, userId: Long) {
        if (!membershipRepository.existsByUserIdAndHouseholdId(userId, householdId)) {
            throw UnauthorizedException("You're not a member of this household")
        }
    }

    private fun requireHouseholdMember(householdId: Long, userId: Long) {
        require(membershipRepository.existsByUserIdAndHouseholdId(userId, householdId)) {
            "User $userId is not a member of this household"
        }
    }
}
