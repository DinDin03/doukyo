package com.doukyo.chat

import org.springframework.data.jpa.repository.JpaRepository

interface MessageReadRepository : JpaRepository<MessageRead, Long> {

    fun findByUserIdAndHouseholdId(userId: Long, householdId: Long): MessageRead?
}
