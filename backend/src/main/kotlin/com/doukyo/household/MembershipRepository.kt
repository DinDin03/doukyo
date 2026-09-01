package com.doukyo.household

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MembershipRepository : JpaRepository<Membership, Long> {

    // `join fetch m.user` loads each membership AND its user in ONE SQL join —
    // the direct fix for the per-member N+1 (otherwise Hibernate would fire a
    // separate SELECT for every member's lazy user).
    @Query("select m from Membership m join fetch m.user where m.household.id = :householdId")
    fun findMembersOfHousehold(@Param("householdId") householdId: Long): List<Membership>

    // Derived query traversing the associations: m.user.id AND m.household.id.
    // Used to stop a user being added to the same household twice.
    fun existsByUserIdAndHouseholdId(userId: Long, householdId: Long): Boolean
}
