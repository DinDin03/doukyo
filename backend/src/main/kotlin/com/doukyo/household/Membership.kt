package com.doukyo.household

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
import java.time.OffsetDateTime

// The join table `memberships` as an entity. It links one User to one Household.
// Its two @ManyToOne references ARE the many-to-many relationship made concrete:
// many memberships point to one user, and many memberships point to one household.
@Entity
@Table(name = "memberships")
class Membership(

    // FetchType.LAZY: don't load the User until it's actually accessed (and then
    // only inside a transaction). @JoinColumn names the FK column (user_id).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    val household: Household,

    @Column(name = "joined_at", nullable = false)
    val joinedAt: OffsetDateTime = OffsetDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
