package com.imanol.gymmanagement.feature.workout.data.remote

import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplate
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutTemplateResponse(
    val id: Long,
    val name: String = "",
    val description: String? = null,
    val active: Boolean = false,
)

fun WorkoutTemplateResponse.toDomain(): WorkoutTemplate =
    WorkoutTemplate(
        id = id,
        name = name,
        description = description,
        active = active,
    )
