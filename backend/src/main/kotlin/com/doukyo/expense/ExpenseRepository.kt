package com.doukyo.expense

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ExpenseRepository : JpaRepository<Expense, Long> {

    // List a household's expenses newest-first, loading their shares in one query
    // (JOIN FETCH) to avoid an N+1 when we render each expense's split.
    // `distinct` because the join multiplies expense rows by their share count.
    @Query(
        "select distinct e from Expense e left join fetch e.shares " +
            "where e.household.id = :householdId order by e.createdAt desc",
    )
    fun findByHouseholdWithShares(@Param("householdId") householdId: Long): List<Expense>
}
