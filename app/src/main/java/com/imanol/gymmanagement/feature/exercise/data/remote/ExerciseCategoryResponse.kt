package com.imanol.gymmanagement.feature.exercise.data.remote

import com.imanol.gymmanagement.feature.exercise.domain.ExerciseCategory
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseCategoryResponse(
    val id: Long,
    val name: String = "",
    val active: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

fun ExerciseCategoryResponse.toDomain(): ExerciseCategory =
    ExerciseCategory(
        id = id,
        name = name,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
