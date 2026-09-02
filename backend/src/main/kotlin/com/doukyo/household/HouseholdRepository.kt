package com.doukyo.household

import org.springframework.data.jpa.repository.JpaRepository

interface HouseholdRepository : JpaRepository<Household, Long> {

    fun existsByInviteCode(code: String): Boolean

    fun findByInviteCode(code: String): Household?
}
