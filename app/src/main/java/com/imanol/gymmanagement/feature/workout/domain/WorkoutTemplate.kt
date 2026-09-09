package com.imanol.gymmanagement.feature.workout.domain

data class WorkoutTemplate(
    val id: Long,
    val name: String,
    val description: String?,
    val active: Boolean,
)
