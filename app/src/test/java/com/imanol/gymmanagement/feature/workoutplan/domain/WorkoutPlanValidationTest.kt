package com.imanol.gymmanagement.feature.workoutplan.domain

import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class WorkoutPlanValidationTest {
    private val exercise = WorkoutPlanExerciseRequest(
        exerciseId = 1L,
        orderIndex = 1,
        sets = 3,
        repetitions = 10,
        restSeconds = 30,
    )

    @Test
    fun acceptsSameStartAndEndDate() {
        assertNull(
            CreateWorkoutPlanRequest(
                startDate = "2026-09-01",
                endDate = "2026-09-01",
                days = listOf(WorkoutPlanDayRequest(1, listOf(exercise))),
            ).validationError(),
        )
    }

    @Test
    fun rejectsEndDateBeforeStartDate() {
        assertNotNull(
            CreateWorkoutPlanRequest(
                startDate = "2026-09-02",
                endDate = "2026-09-01",
                days = listOf(WorkoutPlanDayRequest(1, listOf(exercise))),
            ).validationError(),
        )
    }

    @Test
    fun rejectsInvalidExerciseValues() {
        assertNotNull(
            CreateWorkoutPlanRequest(
                startDate = "2026-09-01",
                days = listOf(WorkoutPlanDayRequest(1, listOf(exercise.copy(sets = 0)))),
            ).validationError(),
        )
    }
}
