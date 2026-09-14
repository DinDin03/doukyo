package com.doukyo.household

import com.doukyo.AbstractIntegrationTest
import com.doukyo.user.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.graphql.execution.ErrorType
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

// The same documents the app sends (HouseholdContext.tsx), so a schema or
// resolver mismatch fails here rather than on a phone.
@AutoConfigureGraphQlTester
class HouseholdGraphQlTest : AbstractIntegrationTest() {

    @Autowired private lateinit var graphQlTester: GraphQlTester
    @Autowired private lateinit var householdService: HouseholdService

    @AfterEach
    fun clearAuth() = SecurityContextHolder.clearContext()

    private fun signedInAs(user: User) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(user.id!!, null, emptyList())
    }

    private fun leave(householdId: Long, confirmDelete: Boolean) =
        graphQlTester.document(
            """
            mutation LeaveHousehold(${'$'}householdId: ID!, ${'$'}confirmDelete: Boolean!) {
              leaveHousehold(householdId: ${'$'}householdId, confirmDelete: ${'$'}confirmDelete)
            }
            """,
        ).variable("householdId", householdId).variable("confirmDelete", confirmDelete).execute()

    @Test
    fun `leave, delete and restore round trip through the API`() {
        val alice = newUser("Alice")
        val household = householdService.createHousehold("Flat 7", alice.id!!)
        signedInAs(alice)

        leave(household.id!!, false).errors().satisfy { errors ->
            assertThat(errors[0].errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(errors[0].message).contains("last member")
        }
        leave(household.id!!, true).path("leaveHousehold").entity(Boolean::class.java).isEqualTo(true)

        graphQlTester.document("query RestorableHouseholds { restorableHouseholds { id name } }")
            .execute().path("restorableHouseholds[*].name").entityList(String::class.java).containsExactly("Flat 7")

        graphQlTester.document(
            """
            mutation RestoreHousehold(${'$'}householdId: ID!) {
              restoreHousehold(householdId: ${'$'}householdId) { id }
            }
            """,
        ).variable("householdId", household.id).execute()
            .path("restoreHousehold.id").entity(String::class.java).isEqualTo(household.id.toString())

        graphQlTester.document("{ myHouseholds { name members { name } } }").execute()
            .path("myHouseholds[0].members[*].name").entityList(String::class.java).containsExactly("Alice")
    }

    @Test
    fun `leaving a household you are not in is FORBIDDEN, not UNAUTHORIZED`() {
        // UNAUTHORIZED signs the app out. A stale screen for a household you just
        // left must get an error, not lose your session.
        val household = householdService.createHousehold("Flat 7", newUser("Alice").id!!)
        signedInAs(newUser("Outsider"))

        leave(household.id!!, false).errors().satisfy { errors ->
            assertThat(errors[0].errorType).isEqualTo(ErrorType.FORBIDDEN)
        }
    }
}
