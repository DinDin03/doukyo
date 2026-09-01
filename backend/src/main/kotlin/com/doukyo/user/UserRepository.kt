package com.doukyo.user

import org.springframework.data.jpa.repository.JpaRepository

// A Spring Data JPA REPOSITORY. Just by extending JpaRepository<Entity, IdType>,
// we get save / findById / findAll / delete / count / ... implemented for us at
// runtime — no code to write.
//
// The two methods below are DERIVED QUERIES: Spring reads the method NAME and
// generates the SQL. `findByEmail` -> "SELECT ... FROM users WHERE email = ?".
// No implementation, no SQL string.
interface UserRepository : JpaRepository<User, Long> {

    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean
}
