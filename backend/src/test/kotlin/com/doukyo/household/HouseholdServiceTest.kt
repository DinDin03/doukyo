package com.doukyo.household

import com.doukyo.AbstractIntegrationTest
import com.doukyo.common.UnauthorizedException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class HouseholdServiceTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var householdService: HouseholdService

    // --- createHousehold ---

    @Test
    fun `creating a household makes the creator its first member`() {
        val alice = newUser("Alice")
        val household = householdService.createHousehold("Flat 7", alice.id!!)

        assertThat(household.name).isEqualTo("Flat 7")
        assertThat(householdService.findMembers(household.id!!).map { it.name }).containsExactly("Alice")
    }

    @Test
    fun `creating a household mints a six character invite code`() {
        val code = householdService.createHousehold("Flat 7", newUser("Alice").id!!).inviteCode
        assertThat(code).hasSize(6)
        // Ambiguous glyphs are excluded so a code can be read aloud or typed from a photo.
        assertThat(code).doesNotContainAnyWhitespaces().matches("[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{6}")
    }

    @Test
    fun `invite codes are unique across households`() {
        val alice = newUser("Alice")
        val codes = (1..25).map { householdService.createHousehold("House $it", alice.id!!).inviteCode }
        assertThat(codes).doesNotHaveDuplicates()
    }

    @Test
    fun `household name is trimmed`() {
        val household = householdService.createHousehold("   Flat 7   ", newUser("Alice").id!!)
        assertThat(household.name).isEqualTo("Flat 7")
    }

    @Test
    fun `blank household names are rejected`() {
        val alice = newUser("Alice")
        listOf("", "   ", "\t\n").forEach { blank ->
            assertThatThrownBy { householdService.createHousehold(blank, alice.id!!) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Please give your household a name")
        }
    }

    @Test
    fun `over-long household names are rejected`() {
        assertThatThrownBy { householdService.createHousehold("x".repeat(61), newUser("Alice").id!!) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("That name is too long")
    }

    @Test
    fun `a 60 character name is accepted at the boundary`() {
        val name = "x".repeat(60)
        assertThat(householdService.createHousehold(name, newUser("Alice").id!!).name).isEqualTo(name)
    }

    @Test
    fun `creating a household for a missing user is unauthorized`() {
        assertThatThrownBy { householdService.createHousehold("Flat 7", 99999L) }
            .isInstanceOf(UnauthorizedException::class.java)
    }

    // --- joinHousehold ---

    @Test
    fun `joining by code adds the user as a member`() {
        val alice = newUser("Alice")
        val bob = newUser("Bob")
        val household = householdService.createHousehold("Flat 7", alice.id!!)

        householdService.joinHousehold(household.inviteCode, bob.id!!)

        assertThat(householdService.findMembers(household.id!!).map { it.name })
            .containsExactlyInAnyOrder("Alice", "Bob")
    }

    @Test
    fun `invite codes are case and whitespace insensitive`() {
        val alice = newUser("Alice")
        val bob = newUser("Bob")
        val code = householdService.createHousehold("Flat 7", alice.id!!).inviteCode

        val joined = householdService.joinHousehold("  ${code.lowercase()}  ", bob.id!!)
        assertThat(joined.name).isEqualTo("Flat 7")
    }

    @Test
    fun `an unknown invite code is rejected`() {
        assertThatThrownBy { householdService.joinHousehold("ZZZZZZ", newUser("Bob").id!!) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("That invite code doesn't match a household")
    }

    @Test
    fun `joining twice is rejected`() {
        val alice = newUser("Alice")
        val household = householdService.createHousehold("Flat 7", alice.id!!)

        assertThatThrownBy { householdService.joinHousehold(household.inviteCode, alice.id!!) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("already a member")
    }

    // --- myHouseholds ---

    @Test
    fun `a new user belongs to no households`() {
        assertThat(householdService.findMyHouseholds(newUser("Nobody").id!!)).isEmpty()
    }

    @Test
    fun `a user sees every household they belong to, and no others`() {
        val alice = newUser("Alice")
        val bob = newUser("Bob")
        val shared = householdService.createHousehold("Shared", alice.id!!)
        householdService.createHousehold("Alice only", alice.id!!)
        householdService.joinHousehold(shared.inviteCode, bob.id!!)

        assertThat(householdService.findMyHouseholds(alice.id!!).map { it.name })
            .containsExactlyInAnyOrder("Shared", "Alice only")
        assertThat(householdService.findMyHouseholds(bob.id!!).map { it.name })
            .containsExactly("Shared")
    }

    // --- findByIdForMember: tenant isolation ---

    @Test
    fun `a member can read their household`() {
        val alice = newUser("Alice")
        val household = householdService.createHousehold("Flat 7", alice.id!!)
        assertThat(householdService.findByIdForMember(household.id!!, alice.id!!)?.name).isEqualTo("Flat 7")
    }

    @Test
    fun `a non-member gets null, not the household`() {
        val alice = newUser("Alice")
        val outsider = newUser("Outsider")
        val household = householdService.createHousehold("Flat 7", alice.id!!)

        assertThat(householdService.findByIdForMember(household.id!!, outsider.id!!)).isNull()
    }

    @Test
    fun `a household that does not exist is indistinguishable from one you cannot see`() {
        val outsider = newUser("Outsider")
        val alice = newUser("Alice")
        val real = householdService.createHousehold("Flat 7", alice.id!!)

        // Both null: an outsider can't probe id ranges to discover which exist.
        assertThat(householdService.findByIdForMember(real.id!!, outsider.id!!)).isNull()
        assertThat(householdService.findByIdForMember(99999L, outsider.id!!)).isNull()
    }
}
