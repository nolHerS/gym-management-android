package com.imanol.gymmanagement.feature.workoutexecution.domain

import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDay
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutExecutionUseCasesTest {
    private val dateProvider = FixedDateProvider("2026-09-09", 1234L)
    private val createSnapshot = CreateWorkoutExecutionSnapshotUseCase(dateProvider)

    @Test
    fun createsSnapshotWithOrderedExercisesAndPlannedSets() {
        val snapshot = createSnapshot(plan(), dayOfWeek = 3)

        assertEquals(10L, snapshot.planId)
        assertEquals(20L, snapshot.clientId)
        assertEquals(1234L, snapshot.startedAt)
        assertEquals(listOf("Press banca", "Sentadilla"), snapshot.exercises.map { it.name })
        assertEquals(listOf(1, 2, 3, 4), snapshot.exercises.first().sets.map { it.setNumber })
        assertEquals(listOf(8, 8, 8, 8), snapshot.exercises.first().sets.map { it.plannedRepetitions })
        assertTrue(snapshot.exercises.all { exercise -> exercise.sets.all { set -> !set.completed } })
    }

    @Test
    fun snapshotCopiesPlannedDataWithoutKeepingPlanCollections() {
        val sourceExercises = mutableListOf(workoutExercise())
        val source = plan(
            days = listOf(WorkoutPlanDay(1L, 3, sourceExercises)),
        )
        val snapshot = createSnapshot(source, dayOfWeek = 3)

        assertEquals(sourceExercises.size, snapshot.exercises.size)
        assertNotSame(sourceExercises, snapshot.exercises)
        assertNotEquals(sourceExercises.first().id.toString(), snapshot.exercises.first().localId)
        sourceExercises.clear()
        assertEquals(1, snapshot.exercises.size)
    }

    @Test
    fun rejectsMissingDayAndEmptyDay() {
        assertReason(WorkoutExecutionValidationReason.DayNotFound) {
            createSnapshot(plan(days = listOf(WorkoutPlanDay(1L, 2, emptyList())), startDate = "2026-09-01"), 3)
        }
        assertReason(WorkoutExecutionValidationReason.DayHasNoExercises) {
            createSnapshot(plan(days = listOf(WorkoutPlanDay(1L, 3, emptyList()))), 3)
        }
    }

    @Test
    fun rejectsInvalidPlanStateDatesExercisesAndOrder() {
        assertReason(WorkoutExecutionValidationReason.PlanNotActive) {
            createSnapshot(plan(status = "COMPLETED"), 3)
        }
        assertReason(WorkoutExecutionValidationReason.PlanOutsideDateRange) {
            createSnapshot(plan(startDate = "2026-09-10"), 3)
        }
        assertReason(WorkoutExecutionValidationReason.InvalidExercise) {
            createSnapshot(plan(exercise = workoutExercise(sets = 0)), 3)
        }
        assertReason(WorkoutExecutionValidationReason.InvalidExerciseOrder) {
            createSnapshot(plan(exercise = workoutExercise(orderIndex = 0)), 3)
        }
    }

    @Test
    fun mapsEveryIsoWeekday() {
        assertEquals(1, isoDayOfWeek("2026-09-07"))
        assertEquals(2, isoDayOfWeek("2026-09-08"))
        assertEquals(3, isoDayOfWeek("2026-09-09"))
        assertEquals(4, isoDayOfWeek("2026-09-10"))
        assertEquals(5, isoDayOfWeek("2026-09-11"))
        assertEquals(6, isoDayOfWeek("2026-09-12"))
        assertEquals(7, isoDayOfWeek("2026-09-13"))
    }

    @Test
    fun usesInjectedDateProviderForTodayAndStartedAt() {
        val snapshot = createSnapshot(plan(), dayOfWeek = isoDayOfWeek(dateProvider.todayIsoDate()))

        assertEquals("2026-09-09", dateProvider.todayIsoDate())
        assertEquals(1234L, snapshot.startedAt)
    }

    @Test
    fun resolvesZeroOneAndMultipleCandidatesExplicitly() {
        val resolver = SelectWorkoutPlanForExecutionUseCase(dateProvider)
        assertTrue(resolver(emptyList(), 3) is WorkoutExecutionPlanSelection.NoCandidate)
        assertTrue(resolver(listOf(plan()), 3) is WorkoutExecutionPlanSelection.Selected)
        assertTrue(
            resolver(listOf(plan(), plan(id = 11L)), 3) is WorkoutExecutionPlanSelection.Ambiguous,
        )
    }

    private fun assertReason(
        expected: WorkoutExecutionValidationReason,
        action: () -> Unit,
    ) {
        try {
            action()
            throw AssertionError("Expected $expected")
        } catch (exception: InvalidWorkoutExecutionException) {
            assertEquals(expected, exception.reason)
        }
    }

    private fun plan(
        id: Long = 10L,
        status: String = "ACTIVE",
        startDate: String = "2026-09-01",
        days: List<WorkoutPlanDay> = listOf(
            WorkoutPlanDay(
                id = 1L,
                dayOfWeek = 3,
                exercises = listOf(
                    workoutExercise(orderIndex = 2, name = "Sentadilla", sets = 3, repetitions = 10),
                    workoutExercise(orderIndex = 1, name = "Press banca", sets = 4, repetitions = 8),
                ),
            ),
        ),
        exercise: WorkoutPlanExercise? = null,
    ) = WorkoutPlan(
        id = id,
        clientId = 20L,
        trainerId = 30L,
        sourceTemplateId = null,
        startDate = startDate,
        endDate = "2026-09-30",
        status = status,
        days = if (exercise == null) days else listOf(WorkoutPlanDay(1L, 3, listOf(exercise))),
    )

    private fun workoutExercise(
        orderIndex: Int = 1,
        name: String = "Press banca",
        sets: Int = 4,
        repetitions: Int = 8,
    ) = WorkoutPlanExercise(
        id = 101L,
        exercise = Exercise(501L, name, null, 1L, "Pecho", true),
        sourceTemplateExerciseId = null,
        orderIndex = orderIndex,
        sets = sets,
        repetitions = repetitions,
        restSeconds = 90,
    )
}

private class FixedDateProvider(
    private val date: String,
    private val timestamp: Long,
) : WorkoutExecutionDateProvider {
    override fun todayIsoDate(): String = date

    override fun nowEpochMillis(): Long = timestamp
}
