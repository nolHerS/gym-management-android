package com.imanol.gymmanagement.feature.exercise.data.remote

import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseResponse(
    val id: Long,
    val name: String = "",
    val description: String? = null,
    val categoryId: Long,
    val categoryName: String = "",
    val active: Boolean = false,
)

fun ExerciseResponse.toDomain(): Exercise =
    Exercise(
        id = id,
        name = name,
        description = description,
        categoryId = categoryId,
        categoryName = categoryName,
        active = active,
    )
