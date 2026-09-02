package com.doukyo.household

import com.doukyo.common.CurrentUser
import com.doukyo.user.User
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class HouseholdController(private val householdService: HouseholdService) {

    @QueryMapping
    fun myHouseholds(): List<Household> = householdService.findMyHouseholds(CurrentUser.id())

    @QueryMapping
    fun household(@Argument id: Long): Household? = householdService.findByIdForMember(id, CurrentUser.id())

    @MutationMapping
    fun createHousehold(@Argument name: String): Household =
        householdService.createHousehold(name, CurrentUser.id())

    @MutationMapping
    fun joinHousehold(@Argument code: String): Household =
        householdService.joinHousehold(code, CurrentUser.id())

    @SchemaMapping(typeName = "Household", field = "members")
    fun members(household: Household): List<User> =
        householdService.findMembers(household.id!!)

    @SchemaMapping(typeName = "Household", field = "createdAt")
    fun createdAt(household: Household): String = household.createdAt.toString()
}
