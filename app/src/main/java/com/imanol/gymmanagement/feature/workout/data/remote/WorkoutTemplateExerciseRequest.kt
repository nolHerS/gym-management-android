package com.imanol.gymmanagement.feature.workout.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutTemplateExerciseRequest(
    val exerciseId: Long,
    val orderIndex: Int,
    val sets: Int,
    val repetitions: Int,
    val restSeconds: Int,
)
