package com.imanol.gymmanagement.feature.exercise.data

import com.imanol.gymmanagement.feature.exercise.data.remote.ExerciseCategoryResponse
import com.imanol.gymmanagement.feature.exercise.data.remote.toDomain
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseCategoryMapperTest {
    @Test
    fun mapsRemoteCategoryToDomain() {
        val response = ExerciseCategoryResponse(
            id = 1L,
            name = "Chest",
            active = true,
            createdAt = "2026-09-01T19:38:30",
            updatedAt = "2026-09-01T19:38:30",
        )

        assertEquals(
            ExerciseCategory(
                id = 1L,
                name = "Chest",
                active = true,
                createdAt = "2026-09-01T19:38:30",
                updatedAt = "2026-09-01T19:38:30",
            ),
            response.toDomain(),
        )
    }
}
