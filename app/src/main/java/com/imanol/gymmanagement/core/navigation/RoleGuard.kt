package com.imanol.gymmanagement.core.navigation

object RoleGuard {
    fun canAccess(role: String?, route: String?): Boolean =
        !requiresTrainerRole(route) || role == "TRAINER"

    private fun requiresTrainerRole(route: String?): Boolean =
        route != null && trainerRoutes.any { route.startsWith(it) }

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
