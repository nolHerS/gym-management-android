package com.imanol.gymmanagement.feature.workout.data.remote

import com.imanol.gymmanagement.feature.exercise.data.remote.ExerciseResponse
import com.imanol.gymmanagement.feature.exercise.data.remote.toDomain
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplateExercise
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutTemplateExerciseResponse(
    val id: Long,
    val exercise: ExerciseResponse,
    val orderIndex: Int,
    val sets: Int,
    val repetitions: Int,
    val restSeconds: Int,
)

fun WorkoutTemplateExerciseResponse.toDomain(): WorkoutTemplateExercise =
    WorkoutTemplateExercise(
        id = id,
        exercise = exercise.toDomain(),
        orderIndex = orderIndex,
        sets = sets,
        repetitions = repetitions,
        restSeconds = restSeconds,
    )
