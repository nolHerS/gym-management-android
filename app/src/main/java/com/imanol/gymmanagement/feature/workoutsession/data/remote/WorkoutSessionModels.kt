package com.imanol.gymmanagement.feature.workoutsession.data.remote

import com.imanol.gymmanagement.feature.nutrition.data.remote.DecimalStringSerializer
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutSessionResponseDto(
    val id: Long,
    val workoutPlanId: Long,
    val clientId: Long,
    val status: String,
    val startedAt: String,
    val finishedAt: String? = null,
    val cancelledAt: String? = null,
    val durationSeconds: Int? = null,
    val notes: String? = null,
    val version: Long,
    val exercises: List<WorkoutSessionExerciseResponseDto> = emptyList(),
)

@Serializable
data class WorkoutSessionExerciseResponseDto(
    val id: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val orderIndex: Int,
    val plannedSets: Int,
    val plannedRepetitions: Int,
    val plannedRestSeconds: Int,
    val completed: Boolean,
    val notes: String? = null,
    val sets: List<WorkoutSessionSetResponseDto> = emptyList(),
)

@Serializable
data class WorkoutSessionSetResponseDto(
    val id: Long,
    val setNumber: Int,
    val plannedRepetitions: Int,
    val actualRepetitions: Int? = null,
    @Serializable(with = DecimalStringSerializer::class)
    val weight: String? = null,
    val rir: Int? = null,
    @Serializable(with = DecimalStringSerializer::class)
    val rpe: String? = null,
    val completed: Boolean,
    val performedAt: String? = null,
)

@Serializable
data class WorkoutSessionCreateRequestDto(val workoutPlanId: Long)

@Serializable
data class WorkoutSessionUpdateRequestDto(
    val notes: String? = null,
    val durationSeconds: Int? = null,
)

@Serializable
data class WorkoutSessionSetRequestDto(
    val sessionExerciseId: Long,
    val setNumber: Int,
    val actualRepetitions: Int? = null,
    @Serializable(with = DecimalStringSerializer::class)
    val weight: String? = null,
    val rir: Int? = null,
    @Serializable(with = DecimalStringSerializer::class)
    val rpe: String? = null,
    val completed: Boolean? = null,
    val performedAt: String? = null,
)

@Serializable
data class WorkoutSessionExerciseUpdateRequestDto(
    val completed: Boolean? = null,
    val notes: String? = null,
)
