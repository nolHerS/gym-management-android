package com.imanol.gymmanagement.feature.workout.data

import com.imanol.gymmanagement.feature.exercise.data.remote.ExerciseResponse
import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.workout.data.remote.WorkoutTemplateExerciseResponse
import com.imanol.gymmanagement.feature.workout.data.remote.WorkoutTemplateResponse
import com.imanol.gymmanagement.feature.workout.data.remote.toDomain
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplate
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplateExercise
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutMapperTest {
    @Test
    fun mapsWorkoutTemplateToDomain() {
        val response = WorkoutTemplateResponse(1L, "Strength", "Three days", true)

        assertEquals(
            WorkoutTemplate(1L, "Strength", "Three days", true),
            response.toDomain(),
        )
    }

    @Test
    fun mapsWorkoutTemplateExerciseAndNestedExerciseToDomain() {
        val response = WorkoutTemplateExerciseResponse(
            id = 4L,
            exercise = ExerciseResponse(2L, "Squat", null, 1L, "Legs", true),
            orderIndex = 1,
            sets = 3,
            repetitions = 10,
            restSeconds = 60,
        )

        assertEquals(
            WorkoutTemplateExercise(
                id = 4L,
                exercise = Exercise(2L, "Squat", null, 1L, "Legs", true),
                orderIndex = 1,
                sets = 3,
                repetitions = 10,
                restSeconds = 60,
            ),
            response.toDomain(),
        )
    }
}
