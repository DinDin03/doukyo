package com.doukyo.expense

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ExpenseRepository : JpaRepository<Expense, Long> {

    // List a household's expenses newest-first, loading their shares in one query
    // (JOIN FETCH) to avoid an N+1 when we render each expense's split.
    // `distinct` because the join multiplies expense rows by their share count.
    // Fetches everything the GraphQL layer will read: the shares, the payer, and
    // each share's user. Resolvers run OUTSIDE the transaction (open-in-view is
    // off), so anything left lazy would throw at serialisation time rather than
    // quietly firing an extra query. Only `shares` is a collection, so there is a
    // single bag fetch — two would be a MultipleBagFetchException.
    @Query(
        "select distinct e from Expense e " +
            "left join fetch e.shares s " +
            "join fetch e.paidBy " +
            "left join fetch s.user " +
            "where e.household.id = :householdId order by e.createdAt desc",
    )
    fun findByHouseholdWithShares(@Param("householdId") householdId: Long): List<Expense>
}
