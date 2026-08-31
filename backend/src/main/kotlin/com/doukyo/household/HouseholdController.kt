package com.doukyo.household

import com.doukyo.user.User
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class HouseholdController(private val householdService: HouseholdService) {

    @QueryMapping
    fun households(): List<Household> = householdService.findAll()

    // @Argument coerces the GraphQL ID (a String like "1") into a Long.
    @QueryMapping
    fun household(@Argument id: Long): Household? = householdService.findById(id)

    @MutationMapping
    fun createHousehold(@Argument name: String): Household =
        householdService.createHousehold(name)

    @MutationMapping
    fun addMember(@Argument householdId: Long, @Argument userId: Long): Household =
        householdService.addMember(householdId, userId)

    // FIELD RESOLVER: resolves `Household.members`. Spring calls this once for each
    // Household in the result, passing that Household. This is the resolver-level
    // N+1 point — fine for now, batched with a DataLoader later.
    @SchemaMapping(typeName = "Household", field = "members")
    fun members(household: Household): List<User> =
        householdService.findMembers(household.id!!)

    // GraphQL has no built-in DateTime scalar, so we expose the timestamp as an
    // ISO-8601 String via a field resolver.
    @SchemaMapping(typeName = "Household", field = "createdAt")
    fun createdAt(household: Household): String = household.createdAt.toString()
}
