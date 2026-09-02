package com.doukyo.household

import com.doukyo.common.UnauthorizedException
import com.doukyo.user.User
import com.doukyo.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HouseholdService(
    private val householdRepository: HouseholdRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository,
) {
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
        val household = householdRepository.findByInviteCode(code.trim().uppercase())
            ?: throw IllegalArgumentException("That invite code doesn't match a household")
        require(!membershipRepository.existsByUserIdAndHouseholdId(userId, household.id!!)) {
            "You're already a member of ${household.name}"
        }
        membershipRepository.save(Membership(user = user, household = household))
        return household
    }

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
