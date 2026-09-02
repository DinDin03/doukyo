package com.doukyo.household

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

// A shared house. Maps to the `households` table.
// Note we do NOT add a `members` collection here (no @OneToMany) — we resolve a
// household's members through the repository/resolver instead (see the concepts).
@Entity
@Table(name = "households")
class Household(

    @Column(nullable = false)
    var name: String,

    // Generated once at creation (HouseholdService), never changes.
    @Column(name = "invite_code", nullable = false, unique = true)
    val inviteCode: String,

    // OffsetDateTime maps to the `timestamptz` column. We set it in code; the DB's
    // DEFAULT now() is the backstop if it's ever omitted.
    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
