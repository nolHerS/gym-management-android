package com.imanol.gymmanagement.feature.workoutsession.data.remote

import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSession
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionExercise
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionExerciseUpdate
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionSet
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionSetInput
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionStatus
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionUpdate

fun WorkoutSessionResponseDto.toDomain() = WorkoutSession(
    id, workoutPlanId, clientId, status.toDomain(), startedAt, finishedAt, cancelledAt,
    durationSeconds, notes, version, exercises.map { it.toDomain() },
)

fun WorkoutSessionExerciseResponseDto.toDomain() = WorkoutSessionExercise(
    id, exerciseId, exerciseName, orderIndex, plannedSets, plannedRepetitions,
    plannedRestSeconds, completed, notes, sets.map { it.toDomain() },
)

fun WorkoutSessionSetResponseDto.toDomain() = WorkoutSessionSet(
    id, setNumber, plannedRepetitions, actualRepetitions, weight, rir, rpe, completed, performedAt,
)

fun String.toDomain(): WorkoutSessionStatus = when (this) {
    "IN_PROGRESS" -> WorkoutSessionStatus.IN_PROGRESS
    "COMPLETED" -> WorkoutSessionStatus.COMPLETED
    "CANCELLED" -> WorkoutSessionStatus.CANCELLED
    else -> throw IllegalArgumentException("Unknown workout session status: $this")
}

fun WorkoutSessionUpdate.toDto() = WorkoutSessionUpdateRequestDto(notes, durationSeconds)

fun WorkoutSessionSetInput.toDto() = WorkoutSessionSetRequestDto(
    sessionExerciseId, setNumber, actualRepetitions, weight, rir, rpe, completed, performedAt,
)

fun WorkoutSessionExerciseUpdate.toDto() = WorkoutSessionExerciseUpdateRequestDto(completed, notes)
