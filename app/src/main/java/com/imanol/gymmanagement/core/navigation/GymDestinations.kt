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
data object ExerciseCategories

@Serializable
data class Exercises(val categoryId: Long)

@Serializable
data class ExerciseDetail(val exerciseId: Long)
