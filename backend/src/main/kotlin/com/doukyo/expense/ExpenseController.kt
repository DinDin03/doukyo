package com.doukyo.expense

import com.doukyo.common.CurrentUser
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class ExpenseController(private val expenseService: ExpenseService) {

    @QueryMapping
    fun expenses(@Argument householdId: Long): List<Expense> =
        expenseService.listExpenses(householdId, CurrentUser.id())

    @QueryMapping
    fun balances(@Argument householdId: Long): List<Balance> =
        expenseService.balances(householdId, CurrentUser.id())

    @MutationMapping
    fun createExpense(
        @Argument householdId: Long,
        @Argument paidById: Long,
        @Argument description: String,
        @Argument amountCents: Long,
        @Argument category: ExpenseCategory,
        @Argument method: SplitMethod,
        @Argument participants: List<ParticipantInput>,
    ): Expense = expenseService.createExpense(
        householdId = householdId,
        callerId = CurrentUser.id(),
        paidById = paidById,
        description = description,
        amountCents = amountCents,
        category = category,
        method = method,
        participants = participants,
    )

    @MutationMapping
    fun settleShare(@Argument shareId: Long): ExpenseShare =
        expenseService.settleShare(shareId, CurrentUser.id())

    // GraphQL has no DateTime scalar, so the timestamp is serialised as ISO-8601 —
    // same as Household.createdAt and Message.createdAt.
    @SchemaMapping(typeName = "Expense", field = "createdAt")
    fun createdAt(expense: Expense): String = expense.createdAt.toString()
}
