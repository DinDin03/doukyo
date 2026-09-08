package com.doukyo.expense

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ExpenseShareRepository : JpaRepository<ExpenseShare, Long> {

    // The balances query. JOIN FETCHes everything it reads — the ower, the expense
    // and its payer — because balances are computed outside a lazy-loading session
    // and each miss would otherwise be one extra SELECT per share.
    //
    // `isPaid = false` is what the partial index in V3 covers: settled debts
    // eventually outnumber unsettled ones, so only the unsettled rows are indexed.
    @Query(
        "select s from ExpenseShare s " +
            "join fetch s.user " +
            "join fetch s.expense e " +
            "join fetch e.paidBy " +
            "where e.household.id = :householdId and s.isPaid = false",
    )
    fun findUnpaidInHousehold(@Param("householdId") householdId: Long): List<ExpenseShare>
}
