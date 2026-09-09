package com.imanol.gymmanagement.feature.workout.domain

import com.imanol.gymmanagement.feature.exercise.domain.Exercise

data class WorkoutTemplateExercise(
    val id: Long,
    val exercise: Exercise,
    val orderIndex: Int,
    val sets: Int,
    val repetitions: Int,
    val restSeconds: Int,
)
