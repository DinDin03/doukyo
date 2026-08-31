package com.doukyo.user

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// The SERVICE layer: business rules + transaction boundaries live here.
// The controller calls this; this calls the repository. Keeping rules here (not in
// the controller) makes them reusable and testable without a web layer.
//
// `private val userRepository: UserRepository` is CONSTRUCTOR INJECTION — Spring
// sees the @Service bean needs a UserRepository and supplies the one it created.
@Service
class UserService(private val userRepository: UserRepository) {

    // readOnly = true: a hint to the DB/Hibernate that this transaction only reads
    // (enables optimizations, prevents accidental writes).
    @Transactional(readOnly = true)
    fun findAll(): List<User> = userRepository.findAll()

    // @Transactional: the whole method runs in ONE transaction. If it throws,
    // everything rolls back (nothing half-written).
    @Transactional
    fun createUser(name: String, email: String): User {
        // Business rule: no duplicate emails. This gives a friendly error before we
        // even hit the DB. The UNIQUE constraint in the schema is the hard backstop
        // (it would reject a duplicate even if this check were removed or raced).
        require(!userRepository.existsByEmail(email)) {
            "A user with email '$email' already exists"
        }
        // save() INSERTs the row and returns the entity now populated with the
        // DB-assigned id.
        return userRepository.save(User(name = name, email = email))
    }
}
