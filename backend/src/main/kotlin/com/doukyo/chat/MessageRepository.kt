package com.doukyo.chat

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

// Every read JOIN FETCHes the sender: the mapper needs sender.name, and without it
// Hibernate would fire one extra SELECT per message (the N+1 we measured earlier).
interface MessageRepository : JpaRepository<Message, Long> {

    @Query("select m from Message m join fetch m.sender where m.household.id = :householdId order by m.id desc")
    fun findLatest(@Param("householdId") householdId: Long, pageable: Pageable): List<Message>

    // Keyset pagination: walk backwards from a cursor instead of OFFSET, which
    // degrades with depth and skips rows when new messages arrive mid-scroll.
    @Query(
        "select m from Message m join fetch m.sender " +
            "where m.household.id = :householdId and m.id < :before order by m.id desc",
    )
    fun findBefore(
        @Param("householdId") householdId: Long,
        @Param("before") before: Long,
        pageable: Pageable,
    ): List<Message>

    // Reconnect backfill: everything the client missed while its socket was closed.
    @Query(
        "select m from Message m join fetch m.sender " +
            "where m.household.id = :householdId and m.id > :after order by m.id asc",
    )
    fun findAfter(@Param("householdId") householdId: Long, @Param("after") after: Long): List<Message>

    fun findByHouseholdIdAndClientId(householdId: Long, clientId: String): Message?

    fun countByHouseholdIdAndIdGreaterThan(householdId: Long, id: Long): Long
}
