package com.imanol.gymmanagement.feature.workoutsession.domain

enum class WorkoutSessionStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

data class WorkoutSession(
    val id: Long,
    val workoutPlanId: Long,
    val clientId: Long,
    val status: WorkoutSessionStatus,
    val startedAt: String,
    val finishedAt: String?,
    val cancelledAt: String?,
    val durationSeconds: Int?,
    val notes: String?,
    val version: Long,
    val exercises: List<WorkoutSessionExercise>,
)

data class WorkoutSessionExercise(
    val id: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val orderIndex: Int,
    val plannedSets: Int,
    val plannedRepetitions: Int,
    val plannedRestSeconds: Int,
    val completed: Boolean,
    val notes: String?,
    val sets: List<WorkoutSessionSet>,
)

data class WorkoutSessionSet(
    val id: Long,
    val setNumber: Int,
    val plannedRepetitions: Int,
    val actualRepetitions: Int?,
    val weight: String?,
    val rir: Int?,
    val rpe: String?,
    val completed: Boolean,
    val performedAt: String?,
)

data class WorkoutSessionUpdate(
    val notes: String? = null,
    val durationSeconds: Int? = null,
)

data class WorkoutSessionSetInput(
    val sessionExerciseId: Long,
    val setNumber: Int,
    val actualRepetitions: Int? = null,
    val weight: String? = null,
    val rir: Int? = null,
    val rpe: String? = null,
    val completed: Boolean? = null,
    val performedAt: String? = null,
)

data class WorkoutSessionExerciseUpdate(
    val completed: Boolean? = null,
    val notes: String? = null,
)
