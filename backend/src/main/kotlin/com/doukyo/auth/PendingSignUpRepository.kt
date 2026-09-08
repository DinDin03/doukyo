package com.doukyo.auth

import org.springframework.data.jpa.repository.JpaRepository

interface PendingSignUpRepository : JpaRepository<PendingSignUp, Long> {

    fun findByEmail(email: String): PendingSignUp?
}
