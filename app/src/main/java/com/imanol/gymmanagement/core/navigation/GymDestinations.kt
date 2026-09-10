package com.imanol.gymmanagement.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data object AuthGraph

@Serializable
data object MainGraph

@Serializable
data object Splash

@Serializable
data object Login

@Serializable
data object Home

@Serializable
data object MyWorkoutPlan

@Serializable
data object ExerciseCategories

@Serializable
data class Exercises(val categoryId: Long)

@Serializable
data class ExerciseDetail(val exerciseId: Long)

@Serializable
data object Clients

@Serializable
data class ClientDetail(val clientId: Long)

@Serializable
data class WorkoutPlans(val clientId: Long)
@Serializable
data class CreateWorkoutPlan(val clientId: Long)

@Serializable
data class WorkoutPlanDetail(val planId: Long)

@Serializable
data object WorkoutTemplates

@Serializable
data class WorkoutTemplateDetail(val templateId: Long)

@Serializable
data class WorkoutTemplateForm(val templateId: Long? = null)

@Serializable
data class NutritionPlans(val clientId: Long)

@Serializable
data class NutritionPlanDetail(val planId: Long)

@Serializable
data class NutritionPlanForm(val clientId: Long, val planId: Long? = null)

@Serializable
data object Foods

@Serializable
data class FoodDetail(val foodId: Long)

@Serializable
data class FoodForm(val foodId: Long? = null)

@Serializable
data object MyNutrition

@Serializable
data class MyNutritionPlan(val planId: Long)
