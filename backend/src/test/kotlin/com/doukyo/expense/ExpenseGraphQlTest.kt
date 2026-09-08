package com.doukyo.expense

import com.doukyo.AbstractIntegrationTest
import com.doukyo.household.Household
import com.doukyo.household.HouseholdService
import com.doukyo.user.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.graphql.execution.ErrorType
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

// Exercises the resolvers, not the service. The reason this layer needs its own
// tests: resolvers serialise OUTSIDE the transaction with open-in-view off, so a
// lazy association that the service never touched throws only here.
@AutoConfigureGraphQlTester
class ExpenseGraphQlTest : AbstractIntegrationTest() {

    @Autowired private lateinit var graphQlTester: GraphQlTester
    @Autowired private lateinit var householdService: HouseholdService

    private lateinit var alice: User
    private lateinit var bob: User
    private lateinit var household: Household

    @BeforeEach
    fun setUpHousehold() {
        alice = newUser("Alice")
        bob = newUser("Bob")
        household = householdService.createHousehold("Flat 7", alice.id!!)
        householdService.joinHousehold(household.inviteCode, bob.id!!)
        signedInAs(alice)
    }

    @AfterEach
    fun clearAuth() = SecurityContextHolder.clearContext()

    private fun signedInAs(user: User) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(user.id!!, null, emptyList())
    }

    private fun createEvenExpense(amountCents: Int = 10_000) =
        graphQlTester.document(
            """
            mutation {
              createExpense(
                householdId: ${household.id}, paidById: ${alice.id},
                description: "Groceries", amountCents: $amountCents,
                category: GROCERIES, method: EVENLY,
                participants: [{ userId: ${alice.id} }, { userId: ${bob.id} }]
              ) { id amountCents category paidBy { name } shares { amountCents isPaid user { name } } }
            }
            """,
        ).execute()

    @Test
    fun `createExpense returns the expense with its payer and shares fully resolved`() {
        // Every one of paidBy, shares and share.user is a lazy association. If any
        // were left unfetched this fails at serialisation, not in the service.
        val result = createEvenExpense()

        result.path("createExpense.amountCents").entity(Int::class.java).isEqualTo(10_000)
        result.path("createExpense.category").entity(String::class.java).isEqualTo("GROCERIES")
        result.path("createExpense.paidBy.name").entity(String::class.java).isEqualTo("Alice")
        result.path("createExpense.shares").entityList(Any::class.java).hasSize(2)
    }

    @Test
    fun `the payer's share comes back already paid`() {
        createEvenExpense()

        val paid = graphQlTester
            .document("{ expenses(householdId: ${household.id}) { shares { isPaid user { name } } } }")
            .execute()
            .path("expenses[0].shares").entityList(Map::class.java).get()
            .associate { (it["user"] as Map<*, *>)["name"] to it["isPaid"] }

        assertThat(paid["Alice"]).isEqualTo(true)
        assertThat(paid["Bob"]).isEqualTo(false)
    }

    @Test
    fun `the expenses query resolves the whole graph without a lazy failure`() {
        createEvenExpense()

        graphQlTester
            .document(
                "{ expenses(householdId: ${household.id}) " +
                    "{ id description amountCents category createdAt paidBy { name email } " +
                    "shares { id amountCents isPaid user { name } } } }",
            )
            .execute()
            .path("expenses[0].createdAt").entity(String::class.java).satisfies {
                assertThat(it).contains("T") // ISO-8601, serialised by the field resolver
            }
    }

    @Test
    fun `balances come back and sum to zero`() {
        createEvenExpense()

        val net = graphQlTester
            .document("{ balances(householdId: ${household.id}) { userName netCents } }")
            .execute()
            .path("balances").entityList(Map::class.java).get()
            .associate { it["userName"] to (it["netCents"] as Int) }

        assertThat(net["Alice"]).isEqualTo(5_000)
        assertThat(net["Bob"]).isEqualTo(-5_000)
        assertThat(net.values.sum()).isZero()
    }

    @Test
    fun `settleShare returns the updated share with its user resolved`() {
        createEvenExpense()
        val shareId = graphQlTester
            .document("{ expenses(householdId: ${household.id}) { shares { id isPaid } } }")
            .execute()
            .path("expenses[0].shares").entityList(Map::class.java).get()
            .first { it["isPaid"] == false }["id"]

        signedInAs(bob)
        graphQlTester
            .document("mutation { settleShare(shareId: $shareId) { isPaid user { name } } }")
            .execute()
            .path("settleShare.isPaid").entity(Boolean::class.java).isEqualTo(true)
    }

    @Test
    fun `the split method enum is accepted from the schema`() {
        val amounts = graphQlTester.document(
            """
            mutation {
              createExpense(
                householdId: ${household.id}, paidById: ${alice.id},
                description: "Rent", amountCents: 10000, category: BILLS, method: WEIGHTS,
                participants: [{ userId: ${alice.id}, value: 3 }, { userId: ${bob.id}, value: 1 }]
              ) { shares { amountCents } }
            }
            """,
        ).execute()
            .path("createExpense.shares[*].amountCents").entityList(Int::class.java).get()

        assertThat(amounts).containsExactlyInAnyOrder(7_500, 2_500)
    }

    @Test
    fun `percentages are rejected when they miss one hundred percent`() {
        graphQlTester.document(
            """
            mutation {
              createExpense(
                householdId: ${household.id}, paidById: ${alice.id},
                description: "Bad", amountCents: 10000, category: OTHER, method: PERCENTAGE,
                participants: [{ userId: ${alice.id}, value: 5000 }, { userId: ${bob.id}, value: 2500 }]
              ) { id }
            }
            """,
        ).execute().errors().satisfy { errors ->
            assertThat(errors[0].errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(errors[0].message).contains("100%")
        }
    }

    @Test
    fun `a non-member is refused by every expense operation`() {
        createEvenExpense()
        signedInAs(newUser("Outsider"))

        listOf(
            "{ expenses(householdId: ${household.id}) { id } }",
            "{ balances(householdId: ${household.id}) { netCents } }",
        ).forEach { document ->
            graphQlTester.document(document).execute().errors().satisfy { errors ->
                assertThat(errors[0].errorType).isEqualTo(ErrorType.UNAUTHORIZED)
            }
        }
    }

    @Test
    fun `expense operations require a signed-in user`() {
        SecurityContextHolder.clearContext()

        graphQlTester
            .document("{ expenses(householdId: ${household.id}) { id } }")
            .execute()
            .errors()
            .satisfy { errors -> assertThat(errors[0].errorType).isEqualTo(ErrorType.UNAUTHORIZED) }
    }
}
