package com.doukyo.expense

import com.doukyo.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

// One person's slice of an expense. Part of the Expense aggregate (owned by it).
// The payer also gets a share, marked paid, so the shares always sum to the total.
@Entity
@Table(name = "expense_shares")
class ExpenseShare(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    val expense: Expense,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "amount_cents", nullable = false)
    var amountCents: Long,

    // "Who hasn't paid?" is a filter on this flag; settling is a one-row update.
    @Column(name = "is_paid", nullable = false)
    var isPaid: Boolean = false,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
