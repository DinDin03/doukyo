package com.doukyo.common

import com.doukyo.AbstractIntegrationTest
import com.doukyo.auth.AuthService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.graphql.execution.ErrorType
import org.springframework.graphql.test.tester.GraphQlTester

// Exercises the real resolver -> exception -> GraphQL error path, not the resolver
// class in isolation: the mapping only matters if it is actually wired in.
@AutoConfigureGraphQlTester
class GraphQlErrorMappingTest : AbstractIntegrationTest() {

    @Autowired private lateinit var graphQlTester: GraphQlTester
    @Autowired private lateinit var authService: AuthService

    @Test
    fun `a broken business rule becomes BAD_REQUEST with the real message`() {
        graphQlTester
            .document("""mutation { signIn(email: "nobody@test.app", password: "whatever") { accessToken } }""")
            .execute()
            .errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertThat(errors[0].errorType).isEqualTo(ErrorType.BAD_REQUEST)
                assertThat(errors[0].message).isEqualTo("Invalid email or password")
            }
    }

    @Test
    fun `a missing signed-in user becomes UNAUTHORIZED`() {
        // No SecurityContext in this thread, so CurrentUser.id() throws.
        graphQlTester
            .document("{ me { id name } }")
            .execute()
            .errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertThat(errors[0].errorType).isEqualTo(ErrorType.UNAUTHORIZED)
                assertThat(errors[0].message).isEqualTo("You are not signed in")
            }
    }

    @Test
    fun `household queries are unauthorized without a signed-in user`() {
        graphQlTester
            .document("{ myHouseholds { id } }")
            .execute()
            .errors()
            .satisfy { errors ->
                assertThat(errors[0].errorType).isEqualTo(ErrorType.UNAUTHORIZED)
            }
    }

    @Test
    fun `validation errors carry the real message for a bad signUp`() {
        graphQlTester
            .document("""mutation { startSignUp(name: "A", email: "a@test.app", password: "short") }""")
            .execute()
            .errors()
            .satisfy { errors ->
                assertThat(errors[0].errorType).isEqualTo(ErrorType.BAD_REQUEST)
                assertThat(errors[0].message).isEqualTo("Password must be at least 8 characters")
            }
    }

    @Test
    fun `the removed users query no longer exists in the schema`() {
        // Deleted deliberately: it returned every account's email to any caller.
        graphQlTester
            .document("{ users { email } }")
            .execute()
            .errors()
            .satisfy { errors ->
                assertThat(errors[0].message).contains("Field 'users' in type 'Query' is undefined")
            }
    }

    @Test
    fun `the removed createUser mutation no longer exists in the schema`() {
        graphQlTester
            .document("""mutation { createUser(name: "Ghost", email: "g@evil.test") { id } }""")
            .execute()
            .errors()
            .satisfy { errors ->
                assertThat(errors[0].message).contains("Field 'createUser' in type 'Mutation' is undefined")
            }
    }

    @Test
    fun `a successful mutation still returns data`() {
        // startSignUp answers true whether or not the address is taken.
        graphQlTester
            .document("""mutation { startSignUp(name: "Alice", email: "alice@test.app", password: "password123") }""")
            .execute()
            .path("startSignUp")
            .entity(Boolean::class.java)
            .isEqualTo(true)
    }
}
