package com.imanol.gymmanagement.feature.workoutexecution.domain

import java.util.UUID

data class WorkoutExecutionSnapshot(
    val planId: Long,
    val clientId: Long,
    val startedAt: Long,
    val exercises: List<WorkoutExecutionExercise>,
)

data class WorkoutExecutionExercise(
    val localId: String = UUID.randomUUID().toString(),
    val exerciseId: Long,
    val name: String,
    val sets: List<WorkoutExecutionSet>,
    val restSeconds: Int,
    val orderIndex: Int,
)

data class WorkoutExecutionSet(
    val setNumber: Int,
    val plannedRepetitions: Int,
    val completed: Boolean = false,
)

data class WorkoutExecutionSetPosition(
    val exerciseIndex: Int,
    val setNumber: Int,
)

enum class WorkoutExecutionStatus {
    Ready,
    Running,
    Resting,
    Finished,
    Cancelled,
}

data class WorkoutExecutionState(
    val status: WorkoutExecutionStatus = WorkoutExecutionStatus.Ready,
    val currentExerciseIndex: Int = 0,
    val currentSetNumber: Int = 1,
    val currentExerciseTotalSets: Int = 0,
    val completedSets: Int = 0,
    val completedSetPositions: Set<WorkoutExecutionSetPosition> = emptySet(),
    val totalExercises: Int = 0,
    val totalSets: Int = 0,
) {
    val currentSetIndex: Int
        get() = (currentSetNumber - 1).coerceAtLeast(0)

    val progress: Float
        get() = if (totalSets == 0) 0f else completedSets.toFloat() / totalSets
}
