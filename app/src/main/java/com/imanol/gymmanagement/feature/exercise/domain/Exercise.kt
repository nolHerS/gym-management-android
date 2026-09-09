package com.imanol.gymmanagement.feature.exercise.domain

data class Exercise(
    val id: Long,
    val name: String,
    val description: String?,
    val categoryId: Long,
    val categoryName: String,
    val active: Boolean,
)
