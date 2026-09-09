package com.imanol.gymmanagement.feature.exercise.data

import com.imanol.gymmanagement.feature.exercise.data.remote.ExerciseResponse
import com.imanol.gymmanagement.feature.exercise.data.remote.toDomain
import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseMapperTest {
    @Test
    fun mapsRemoteExerciseToDomain() {
        val response = ExerciseResponse(1L, "Bench Press", "Barbell bench press", 2L, "Chest", true)

        assertEquals(
            Exercise(1L, "Bench Press", "Barbell bench press", 2L, "Chest", true),
            response.toDomain(),
        )
    }
}
