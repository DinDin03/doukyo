package com.doukyo.household

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface HouseholdRepository : JpaRepository<Household, Long> {

    fun existsByInviteCode(code: String): Boolean

    // Locked so a join and the last member leaving take turns. If the leave wins,
    // Postgres re-checks this WHERE once the lock frees: the code has rotated and
    // deleted_at is set, so the join finds nothing instead of a dying household.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByInviteCodeAndDeletedAtIsNull(code: String): Household?

    // Every change to who is in a household takes this lock first.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Household h where h.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Household?

    fun findByDeletedByAndDeletedAtAfter(userId: Long, cutoff: OffsetDateTime): List<Household>
}
