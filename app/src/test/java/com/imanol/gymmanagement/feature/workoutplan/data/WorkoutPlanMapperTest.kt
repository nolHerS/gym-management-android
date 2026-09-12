package com.imanol.gymmanagement.feature.workoutplan.data

import com.imanol.gymmanagement.feature.exercise.data.remote.ExerciseResponse
import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.workoutplan.data.remote.WorkoutPlanExerciseResponse
import com.imanol.gymmanagement.feature.workoutplan.data.remote.WorkoutPlanDayResponse
import com.imanol.gymmanagement.feature.workoutplan.data.remote.WorkoutPlanResponse
import com.imanol.gymmanagement.feature.workoutplan.data.remote.toDomain
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutPlanMapperTest {
    @Test
    fun mapsDayResponseToDomain() {
        val day = WorkoutPlanDayResponse(
            id = 9L,
            dayOfWeek = 2,
            exercises = emptyList(),
        ).toDomain()

        assertEquals(9L, day.id)
        assertEquals(2, day.dayOfWeek)
        assertEquals(emptyList<Any>(), day.exercises)
    }

    @Test
    fun mapsExerciseResponseToDomain() {
        val exercise = WorkoutPlanExerciseResponse(
            id = 10L,
            exercise = null,
            sourceTemplateExerciseId = 12L,
            orderIndex = 1,
            sets = 3,
            repetitions = 10,
            restSeconds = 60,
        ).toDomain()

        assertEquals(10L, exercise.id)
        assertEquals(12L, exercise.sourceTemplateExerciseId)
        assertEquals(1, exercise.orderIndex)
        assertEquals(3, exercise.sets)
        assertEquals(10, exercise.repetitions)
        assertEquals(60, exercise.restSeconds)
    }

    @Test
    fun mapsNestedPlanAndExerciseToDomain() {
        val response = WorkoutPlanResponse(
            id = 8L,
            clientId = 2L,
            trainerId = 1L,
            sourceTemplateId = 4L,
            startDate = "2026-09-01",
            endDate = "2026-10-01",
            status = "ACTIVE",
            days = listOf(
                com.imanol.gymmanagement.feature.workoutplan.data.remote.WorkoutPlanDayResponse(
                    9L,
                    1,
                    listOf(
                        WorkoutPlanExerciseResponse(
                            10L,
                            ExerciseResponse(3L, "Press", null, 5L, "Chest", true),
                            12L,
                            1,
                            3,
                            10,
                            60,
                        ),
                    ),
                ),
            ),
        )

        val plan = response.toDomain()

        assertEquals(8L, plan.id)
        assertEquals(1, plan.days.single().dayOfWeek)
        assertEquals(
            Exercise(3L, "Press", null, 5L, "Chest", true),
            plan.days.single().exercises.single().exercise,
        )
    }
}
