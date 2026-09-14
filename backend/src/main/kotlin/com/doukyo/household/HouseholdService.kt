package com.doukyo.household

import com.doukyo.common.ForbiddenException
import com.doukyo.common.UnauthorizedException
import com.doukyo.expense.ExpenseShareRepository
import com.doukyo.user.User
import com.doukyo.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class HouseholdService(
    private val householdRepository: HouseholdRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository,
    private val expenseShareRepository: ExpenseShareRepository,
) {
    companion object {
        const val RESTORE_WINDOW_DAYS = 30L
        const val NOT_RESTORABLE = "That household can't be restored"
    }

    // Excludes 0/O/1/I/L — characters people misread when a code is read aloud
    // or typed from a photo of a screen.
    private val codeChars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    @Transactional(readOnly = true)
    fun findMyHouseholds(userId: Long): List<Household> =
        membershipRepository.findHouseholdsForUser(userId).map { it.household }

    // Returns null for "doesn't exist" AND "you're not a member" alike — we never
    // reveal that a household exists to someone who isn't in it.
    @Transactional(readOnly = true)
    fun findByIdForMember(id: Long, userId: Long): Household? {
        val household = householdRepository.findByIdOrNull(id) ?: return null
        return household.takeIf { membershipRepository.existsByUserIdAndHouseholdId(userId, id) }
    }

    @Transactional
    fun createHousehold(name: String, userId: Long): Household {
        val cleanName = name.trim()
        require(cleanName.isNotBlank()) { "Please give your household a name" }
        require(cleanName.length <= 60) { "That name is too long" }
        val user = requireUser(userId)
        val household = householdRepository.save(Household(name = cleanName, inviteCode = generateUniqueInviteCode()))
        membershipRepository.save(Membership(user = user, household = household))
        return household
    }

    @Transactional
    fun joinHousehold(code: String, userId: Long): Household {
        val user = requireUser(userId)
        val household = householdRepository.findByInviteCodeAndDeletedAtIsNull(code.trim().uppercase())
            ?: throw IllegalArgumentException("That invite code doesn't match a household")
        require(!membershipRepository.existsByUserIdAndHouseholdId(userId, household.id!!)) {
            "You're already a member of ${household.name}"
        }
        membershipRepository.save(Membership(user = user, household = household))
        return household
    }

    // The household row is locked before anything is counted. Without it, the last
    // two members leaving at once each see the other still there, both go, and the
    // household is left with nobody in it and no deletedBy to restore it.
    @Transactional
    fun leaveHousehold(householdId: Long, userId: Long, confirmDelete: Boolean) {
        val household = householdRepository.findByIdForUpdate(householdId)
        if (household == null || !membershipRepository.existsByUserIdAndHouseholdId(userId, householdId)) {
            throw ForbiddenException("You're not a member of this household")
        }
        require(!expenseShareRepository.hasOpenDebts(householdId, userId)) {
            "Settle up first — you still have unpaid expenses with housemates"
        }
        val lastMember = membershipRepository.countByHouseholdId(householdId) == 1L
        // Checked here, not just in the app: an old build would skip the prompt.
        require(!lastMember || confirmDelete) {
            "You're the last member, so leaving will delete ${household.name}"
        }

        membershipRepository.deleteByUserIdAndHouseholdId(userId, householdId)
        household.inviteCode = generateUniqueInviteCode()
        if (lastMember) {
            household.deletedAt = OffsetDateTime.now()
            household.deletedBy = userId
        }
    }

    @Transactional(readOnly = true)
    fun findRestorableHouseholds(userId: Long): List<Household> =
        householdRepository.findByDeletedByAndDeletedAtAfter(userId, restoreCutoff())

    // One message for missing, someone else's, expired and not deleted: restore
    // must not tell a caller which household ids exist.
    @Transactional
    fun restoreHousehold(householdId: Long, userId: Long): Household {
        val household = householdRepository.findByIdForUpdate(householdId)
        val restorable = household?.deletedBy == userId && household.deletedAt?.isAfter(restoreCutoff()) == true
        require(restorable) { NOT_RESTORABLE }

        household!!.deletedAt = null
        household.deletedBy = null
        membershipRepository.save(Membership(user = requireUser(userId), household = household))
        return household
    }

    // ponytail: expired households are hidden, never purged. Add a scheduled purge when storage matters.
    private fun restoreCutoff(): OffsetDateTime = OffsetDateTime.now().minusDays(RESTORE_WINDOW_DAYS)

    @Transactional(readOnly = true)
    fun findMembers(householdId: Long): List<User> =
        membershipRepository.findMembersOfHousehold(householdId).map { it.user }

    private fun requireUser(userId: Long): User =
        userRepository.findByIdOrNull(userId) ?: throw UnauthorizedException("This account no longer exists")

    private fun generateUniqueInviteCode(): String {
        while (true) {
            val code = (1..6).map { codeChars.random() }.joinToString("")
            if (!householdRepository.existsByInviteCode(code)) return code
        }
    }
}
