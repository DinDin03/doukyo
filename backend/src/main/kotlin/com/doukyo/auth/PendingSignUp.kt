package com.doukyo.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

// A sign-up in flight. Becomes a User only when the emailed code comes back.
@Entity
@Table(name = "pending_signups")
class PendingSignUp(

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var name: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "code_hash", nullable = false)
    var codeHash: String,

    @Column(nullable = false)
    var attempts: Int = 0,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: OffsetDateTime,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
