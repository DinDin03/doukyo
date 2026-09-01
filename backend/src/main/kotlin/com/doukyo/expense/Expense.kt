package com.doukyo.expense

import com.doukyo.household.Household
import com.doukyo.user.User
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.OffsetDateTime

// The Expense AGGREGATE ROOT. It owns its shares — they are created, loaded and
// deleted with it — so unlike Membership, here a bidirectional @OneToMany with
// cascade is the honest model of the aggregate boundary.
@Entity
@Table(name = "expenses")
class Expense(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    val household: Household,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by", nullable = false)
    val paidBy: User,

    @Column(nullable = false)
    var description: String,

    // Money as integer cents — exact arithmetic, never floating point.
    @Column(name = "amount_cents", nullable = false)
    var amountCents: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var category: ExpenseCategory,

    // cascade = ALL + orphanRemoval: saving the expense saves its shares; deleting
    // it deletes them; clearing/replacing the list removes the old rows. All one
    // transaction — which is what keeps "shares sum to the total" atomic.
    @OneToMany(mappedBy = "expense", cascade = [CascadeType.ALL], orphanRemoval = true)
    val shares: MutableList<ExpenseShare> = mutableListOf(),

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
