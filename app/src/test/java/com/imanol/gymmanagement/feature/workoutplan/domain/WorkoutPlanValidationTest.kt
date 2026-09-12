package com.imanol.gymmanagement.feature.workoutplan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun rejectsDuplicateDayAndOrderInCreateRequest() {
        val duplicateDays = CreateWorkoutPlanRequest(
            startDate = "2026-09-01",
            days = listOf(
                WorkoutPlanDayRequest(1, listOf(exercise)),
                WorkoutPlanDayRequest(1, listOf(exercise.copy(orderIndex = 2))),
            ),
        )
        val duplicateOrder = CreateWorkoutPlanRequest(
            startDate = "2026-09-01",
            days = listOf(
                WorkoutPlanDayRequest(
                    1,
                    listOf(exercise, exercise.copy(orderIndex = 1, exerciseId = 2L)),
                ),
            ),
        )

        assertEquals("No puede haber días duplicados.", duplicateDays.validationError())
        assertEquals(
            "El orden de ejercicios no puede repetirse en un mismo día.",
            duplicateOrder.validationError(),
        )
    }

    @Test
    fun draftOperationsSupportAddUpdateDeleteAndValidation() {
        val request = WorkoutPlanExerciseRequest(
            exerciseId = 3L,
            orderIndex = 1,
            sets = 4,
            repetitions = 8,
            restSeconds = 45,
        )

        val dayDraft = WorkoutPlanDraft(clientId = 2L, startDate = "2026-09-01").addDay(3)
        val dayLocalId = dayDraft.days.single().localId
        val draft = dayDraft
            .upsertExercise(dayLocalId, WorkoutPlanDraftExercise(1L, request, "Press"))
            .upsertExercise(dayLocalId, WorkoutPlanDraftExercise(1L, request.copy(sets = 5), "Press"))

        assertEquals(1, draft.days.single().exercises.size)
        assertEquals(5, draft.days.single().exercises.single().request.sets)
        assertNull(draft.validationError())

        val invalid = draft
            .addDay(1)
            .removeExercise(1L)

        assertEquals("Añade al menos un ejercicio a cada día.", invalid.validationError())
    }

    @Test
    fun draftDaysHaveLocalIdsAndRequestsDoNotExposeThem() {
        val draft = WorkoutPlanDraft(clientId = 2L, startDate = "2026-09-01")
            .addDay(1)
            .addDay(3)
        val days = draft.days

        assertEquals(2, days.map { it.localId }.toSet().size)
        assertEquals(listOf(1, 3), draft.toRequests().map { it.dayOfWeek })
    }

    @Test
    fun fromScratchRejectsTemplateExerciseReferences() {
        val dayDraft = WorkoutPlanDraft(clientId = 2L, startDate = "2026-09-01").addDay(1)
        val draft = dayDraft
            .upsertExercise(
                dayDraft.days.single().localId,
                WorkoutPlanDraftExercise(
                    localId = 1L,
                    request = exercise.copy(
                        exerciseId = null,
                        sourceTemplateExerciseId = 7L,
                    ),
                ),
            )

        assertNotNull(draft.validationError())
    }
}
