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

    // Settling returns the updated share to the client, so its user must be loaded
    // before the transaction closes. The household comes along for the membership
    // check that authorises the settle.
    @Query(
        "select s from ExpenseShare s " +
            "join fetch s.user " +
            "join fetch s.expense e " +
            "join fetch e.household " +
            "where s.id = :id",
    )
    fun findByIdWithDetails(@Param("id") id: Long): ExpenseShare?

    // Any unpaid debt between this user and someone else, in either direction.
    // Deliberately not "net balance is zero": owing Bob and being owed by Carol
    // nets to zero while Bob is still out of pocket.
    @Query(
        "select count(s) > 0 from ExpenseShare s join s.expense e " +
            "where e.household.id = :householdId and s.isPaid = false " +
            "and s.user.id <> e.paidBy.id " +
            "and (s.user.id = :userId or e.paidBy.id = :userId)",
    )
    fun hasOpenDebts(@Param("householdId") householdId: Long, @Param("userId") userId: Long): Boolean
}
