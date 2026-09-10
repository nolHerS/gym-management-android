package com.imanol.gymmanagement.feature.workoutplan.data

import com.imanol.gymmanagement.feature.exercise.data.remote.ExerciseResponse
import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.workoutplan.data.remote.WorkoutPlanExerciseResponse
import com.imanol.gymmanagement.feature.workoutplan.data.remote.WorkoutPlanResponse
import com.imanol.gymmanagement.feature.workoutplan.data.remote.toDomain
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutPlanMapperTest {
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
