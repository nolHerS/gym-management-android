package com.imanol.gymmanagement.feature.workout.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutTemplateRequest(
    val name: String,
    val description: String? = null,
)
