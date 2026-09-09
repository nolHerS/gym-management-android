package com.imanol.gymmanagement.feature.exercise.domain

data class ExerciseCategory(
    val id: Long,
    val name: String,
    val active: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
)
