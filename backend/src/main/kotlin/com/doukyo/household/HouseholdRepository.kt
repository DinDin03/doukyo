package com.doukyo.household

import org.springframework.data.jpa.repository.JpaRepository

// Plain CRUD for households — save/findById/findAll/... all free.
interface HouseholdRepository : JpaRepository<Household, Long>
