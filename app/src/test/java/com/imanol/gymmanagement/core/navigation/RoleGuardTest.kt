package com.imanol.gymmanagement.core.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleGuardTest {
    @Test
    fun trainerCanAccessTrainerDestination() {
        assertTrue(RoleGuard.canAccess("TRAINER", Clients::class.qualifiedName))
    }

    @Test
    fun clientCannotAccessTrainerDestination() {
        assertFalse(RoleGuard.canAccess("CLIENT", Clients::class.qualifiedName))
    }

    @Test
    fun clientCanAccessClientDestination() {
        assertTrue(RoleGuard.canAccess("CLIENT", MyWorkoutPlan::class.qualifiedName))
    }

    @Test
    fun trainerCannotAccessClientDestination() {
        assertFalse(RoleGuard.canAccess("TRAINER", MyWorkoutPlan::class.qualifiedName))
    }

    @Test
    fun bothRolesCanAccessSharedDestination() {
        assertTrue(RoleGuard.canAccess("CLIENT", Home::class.qualifiedName))
        assertTrue(RoleGuard.canAccess("TRAINER", Home::class.qualifiedName))
    }

    @Test
    fun unknownDestinationIsNotBlockedByRoleGuard() {
        assertTrue(RoleGuard.canAccess("CLIENT", Home::class.qualifiedName))
    }
}
