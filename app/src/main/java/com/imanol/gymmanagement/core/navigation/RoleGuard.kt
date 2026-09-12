package com.imanol.gymmanagement.core.navigation

object RoleGuard {
    fun canAccess(role: String?, route: String?): Boolean =
        when {
            requiresTrainerRole(route) -> role == "TRAINER"
            requiresClientRole(route) -> role == "CLIENT"
            else -> true
        }

    private fun requiresTrainerRole(route: String?): Boolean =
        route != null && trainerRoutes.any { route.startsWith(it) }

    private fun requiresClientRole(route: String?): Boolean =
        route != null && clientRoutes.any { route.startsWith(it) }

    private val clientRoutes = setOf(
        MyWorkoutPlan::class.qualifiedName,
    ).filterNotNull()

    private val trainerRoutes = setOf(
        Clients::class.qualifiedName,
        ClientDetail::class.qualifiedName,
        WorkoutPlans::class.qualifiedName,
        WorkoutPlanForm::class.qualifiedName,
        WorkoutPlanDetail::class.qualifiedName,
        WorkoutTemplates::class.qualifiedName,
        WorkoutTemplateDetail::class.qualifiedName,
        WorkoutTemplateForm::class.qualifiedName,
        NutritionPlans::class.qualifiedName,
        NutritionPlanDetail::class.qualifiedName,
        NutritionPlanForm::class.qualifiedName,
        FoodForm::class.qualifiedName,
    ).filterNotNull()
}
