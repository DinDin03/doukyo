package com.doukyo.household

import com.doukyo.user.User
import com.doukyo.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// Business logic for households and memberships. It depends on UserRepository too
// (a household operation needs to look up users) — a normal cross-module dependency
// inside the modular monolith.
@Service
class HouseholdService(
    private val householdRepository: HouseholdRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun findAll(): List<Household> = householdRepository.findAll()

    @Transactional(readOnly = true)
    fun findById(id: Long): Household? = householdRepository.findByIdOrNull(id)

    @Transactional
    fun createHousehold(name: String): Household =
        householdRepository.save(Household(name = name))

    @Transactional
    fun addMember(householdId: Long, userId: Long): Household {
        // Validate both ends exist — friendly BAD_REQUEST instead of an FK crash.
        val household = householdRepository.findByIdOrNull(householdId)
            ?: throw IllegalArgumentException("No household with id $householdId")
        val user = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("No user with id $userId")

        // Business rule: can't join the same household twice. The UNIQUE
        // (user_id, household_id) constraint is the hard backstop.
        require(!membershipRepository.existsByUserIdAndHouseholdId(userId, householdId)) {
            "User $userId is already a member of household $householdId"
        }

        membershipRepository.save(Membership(user = user, household = household))
        return household
    }

    // Runs inside a read-only transaction; the JOIN FETCH query means each
    // membership's user is already loaded, so mapping to it is safe and cheap.
    @Transactional(readOnly = true)
    fun findMembers(householdId: Long): List<User> =
        membershipRepository.findMembersOfHousehold(householdId).map { it.user }
}
