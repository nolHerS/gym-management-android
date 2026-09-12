package com.imanol.gymmanagement.feature.workoutexecution.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutExecutionUseCaseTest {
    @Test
    fun startsOnlyFromReadyAndStartIsIdempotent() {
        val execution = WorkoutExecutionUseCase(snapshot())

        assertEquals(WorkoutExecutionStatus.Ready, execution.state.status)
        assertEquals(0, execution.state.currentExerciseIndex)
        assertEquals(0, execution.state.currentSetIndex)
        assertEquals(2, execution.state.totalExercises)
        assertEquals(3, execution.state.totalSets)

        assertEquals(WorkoutExecutionStatus.Running, execution.start().status)
        assertEquals(WorkoutExecutionStatus.Running, execution.start().status)
        assertEquals(0, execution.state.currentExerciseIndex)
    }

    @Test
    fun completesSetsAndEntersRestWithoutChangingPlannedData() {
        val execution = WorkoutExecutionUseCase(snapshot(restSeconds = 60))
        execution.start()

        val state = execution.completeCurrentSet()

        assertEquals(WorkoutExecutionStatus.Resting, state.status)
        assertEquals(1, state.completedSets)
        assertEquals(
            setOf(WorkoutExecutionSetPosition(0, 1)),
            state.completedSetPositions,
        )
        assertEquals(1, state.currentSetNumber)
        assertEquals(8, snapshot(restSeconds = 60).exercises.first().sets.first().plannedRepetitions)
        assertEquals(60, snapshot(restSeconds = 60).exercises.first().restSeconds)
        assertEquals(1f / 3f, state.progress, 0.0001f)
    }

    @Test
    fun finishRestMovesToNextSetAndDoesNotSkipSeries() {
        val execution = WorkoutExecutionUseCase(snapshot(restSeconds = 60))
        execution.start()
        execution.completeCurrentSet()

        assertEquals(WorkoutExecutionStatus.Running, execution.finishRest().status)
        assertEquals(1, execution.state.currentSetIndex)
        assertEquals(0, execution.state.currentExerciseIndex)

        assertEquals(
            WorkoutExecutionStatus.Resting,
            execution.completeCurrentSet().status,
        )
        assertEquals(1, execution.state.currentSetIndex)
    }

    @Test
    fun invalidOperationsAreSafeNoOps() {
        val execution = WorkoutExecutionUseCase(snapshot(restSeconds = 60))

        assertEquals(WorkoutExecutionStatus.Ready, execution.completeCurrentSet().status)
        assertEquals(WorkoutExecutionStatus.Ready, execution.finishRest().status)
        assertEquals(WorkoutExecutionStatus.Ready, execution.finish().status)
        execution.start()
        execution.completeCurrentSet()
        assertEquals(WorkoutExecutionStatus.Resting, execution.completeCurrentSet().status)
        assertEquals(WorkoutExecutionStatus.Running, execution.finishRest().status)
    }

    @Test
    fun movesToNextExerciseAtSeriesBoundaryAndStartsAtSetOne() {
        val execution = WorkoutExecutionUseCase(snapshot(firstExerciseSets = 1, restSeconds = 0))
        execution.start()

        val state = execution.completeCurrentSet()

        assertEquals(WorkoutExecutionStatus.Running, state.status)
        assertEquals(1, state.currentExerciseIndex)
        assertEquals(1, state.currentSetNumber)
        assertEquals(1, state.currentExerciseTotalSets)
        assertEquals(1, state.completedSets)
    }

    @Test
    fun lastSeriesFinishesWithOrWithoutRest() {
        val withoutRest = WorkoutExecutionUseCase(snapshot(firstExerciseSets = 1, secondExerciseSets = 1, restSeconds = 0))
        withoutRest.start()
        withoutRest.completeCurrentSet()
        assertEquals(WorkoutExecutionStatus.Running, withoutRest.state.status)
        withoutRest.completeCurrentSet()
        assertEquals(WorkoutExecutionStatus.Finished, withoutRest.state.status)
        assertEquals(1f, withoutRest.state.progress, 0.0001f)
        assertEquals(WorkoutExecutionStatus.Finished, withoutRest.finish().status)
        assertEquals(WorkoutExecutionStatus.Finished, withoutRest.start().status)

        val withRest = WorkoutExecutionUseCase(snapshot(firstExerciseSets = 1, secondExerciseSets = 1, restSeconds = 30))
        withRest.start()
        withRest.completeCurrentSet()
        withRest.finishRest()
        withRest.completeCurrentSet()
        assertEquals(WorkoutExecutionStatus.Resting, withRest.state.status)
        assertEquals(WorkoutExecutionStatus.Finished, withRest.finishRest().status)
        assertEquals(WorkoutExecutionStatus.Finished, withRest.finishRest().status)
    }

    @Test
    fun cancellationStopsEveryFollowingOperation() {
        val execution = WorkoutExecutionUseCase(snapshot())

        execution.cancel()
        val cancelled = execution.state
        execution.start()
        execution.completeCurrentSet()
        execution.finishRest()
        execution.finish()

        assertEquals(WorkoutExecutionStatus.Cancelled, execution.state.status)
        assertEquals(cancelled, execution.state)
    }

    @Test
    fun cancellationWorksFromRunningAndResting() {
        val running = WorkoutExecutionUseCase(snapshot())
        running.start()
        assertEquals(WorkoutExecutionStatus.Cancelled, running.cancel().status)

        val resting = WorkoutExecutionUseCase(snapshot(restSeconds = 60))
        resting.start()
        resting.completeCurrentSet()
        assertEquals(WorkoutExecutionStatus.Cancelled, resting.cancel().status)
        assertEquals(WorkoutExecutionStatus.Cancelled, resting.finishRest().status)
    }

    @Test
    fun emptyAndZeroSetSnapshotsDoNotThrow() {
        val empty = WorkoutExecutionUseCase(
            WorkoutExecutionSnapshot(1L, 2L, 3L, emptyList()),
        )
        assertEquals(0, empty.state.totalSets)
        assertEquals(0f, empty.state.progress, 0.0001f)
        assertEquals(WorkoutExecutionStatus.Ready, empty.start().status)

        val zeroSets = WorkoutExecutionUseCase(
            WorkoutExecutionSnapshot(
                1L,
                2L,
                3L,
                listOf(
                    WorkoutExecutionExercise(
                        localId = "empty",
                        exerciseId = 1L,
                        name = "Vacío",
                        sets = emptyList(),
                        restSeconds = 0,
                        orderIndex = 1,
                    ),
                ),
            ),
        )
        assertEquals(WorkoutExecutionStatus.Ready, zeroSets.start().status)
        assertFalse(zeroSets.state.progress > 0f)
    }
}

private fun snapshot(
    firstExerciseSets: Int = 2,
    secondExerciseSets: Int = 1,
    restSeconds: Int = 0,
) = WorkoutExecutionSnapshot(
    planId = 1L,
    clientId = 2L,
    startedAt = 3L,
    exercises = listOf(
        WorkoutExecutionExercise(
            localId = "first",
            exerciseId = 10L,
            name = "Press",
            sets = (1..firstExerciseSets).map { WorkoutExecutionSet(it, 8) },
            restSeconds = restSeconds,
            orderIndex = 1,
        ),
        WorkoutExecutionExercise(
            localId = "second",
            exerciseId = 11L,
            name = "Sentadilla",
            sets = (1..secondExerciseSets).map { WorkoutExecutionSet(it, 10) },
            restSeconds = restSeconds,
            orderIndex = 2,
        ),
    ),
)
