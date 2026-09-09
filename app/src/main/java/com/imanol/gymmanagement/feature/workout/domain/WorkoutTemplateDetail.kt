package com.imanol.gymmanagement.feature.workout.domain

data class WorkoutTemplateDetail(
    val template: WorkoutTemplate,
    val exercises: List<WorkoutTemplateExercise>,
)
