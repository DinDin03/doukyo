package com.doukyo.auth

import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    // We look a token up by its hash (we're handed the raw token, hash it, then
    // find the row). Derived query — Spring writes the SQL from the method name.
    fun findByTokenHash(tokenHash: String): RefreshToken?
}
