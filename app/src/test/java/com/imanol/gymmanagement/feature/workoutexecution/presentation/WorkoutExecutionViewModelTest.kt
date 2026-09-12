package com.imanol.gymmanagement.feature.workoutexecution.presentation

import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionSnapshot
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionExercise
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionSet
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionStatus
import com.imanol.gymmanagement.feature.workoutexecution.domain.MonotonicClock
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutRestTimerStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutExecutionViewModelTest {
    @Test
    fun exposesStateAndDelegatesOperationsWithoutLosingProgress() {
        val viewModel = WorkoutExecutionViewModel(snapshot())

        assertEquals(WorkoutExecutionStatus.Ready, viewModel.uiState.value.status)
        viewModel.start()
        viewModel.completeCurrentSet()

        assertEquals(WorkoutExecutionStatus.Running, viewModel.uiState.value.status)
        assertEquals(1, viewModel.uiState.value.completedSets)
        assertEquals(1, viewModel.uiState.value.currentSetIndex)

        viewModel.completeCurrentSet()
        assertEquals(WorkoutExecutionStatus.Finished, viewModel.uiState.value.status)
        viewModel.finish()
        viewModel.cancel()
        assertEquals(WorkoutExecutionStatus.Finished, viewModel.uiState.value.status)
    }

    @Test
    fun restAndCancelAreExposedThroughViewModel() {
        val viewModel = WorkoutExecutionViewModel(
            snapshot(restSeconds = 60),
            clock = FakeMonotonicClock(1_000L),
            tickerEnabled = false,
        )

        viewModel.start()
        viewModel.completeCurrentSet()
        assertEquals(WorkoutExecutionStatus.Resting, viewModel.uiState.value.status)
        viewModel.finishRest()
        assertEquals(WorkoutExecutionStatus.Running, viewModel.uiState.value.status)
        viewModel.cancel()
        assertEquals(WorkoutExecutionStatus.Cancelled, viewModel.uiState.value.status)
        viewModel.finishRest()
        assertEquals(WorkoutExecutionStatus.Cancelled, viewModel.uiState.value.status)
    }

    @Test
    fun restTimerUpdatesAndWaitsForManualContinuationAfterFinishing() {
        val clock = FakeMonotonicClock(1_000L)
        val viewModel = WorkoutExecutionViewModel(
            snapshot(restSeconds = 60),
            clock = clock,
            tickerEnabled = false,
        )

        viewModel.start()
        viewModel.completeCurrentSet()
        assertEquals(60_000L, viewModel.restTimerState.value.remainingMillis)

        clock.now = 21_000L
        viewModel.refreshRestTimer()
        assertEquals(40_000L, viewModel.restTimerState.value.remainingMillis)

        clock.now = 61_000L
        viewModel.refreshRestTimer()

        assertEquals(WorkoutRestTimerStatus.Finished, viewModel.restTimerState.value.status)
        assertEquals(WorkoutExecutionStatus.Resting, viewModel.uiState.value.status)
        assertEquals(0, viewModel.uiState.value.currentSetIndex)
        viewModel.finishRest()
        assertEquals(WorkoutExecutionStatus.Running, viewModel.uiState.value.status)
        assertEquals(1, viewModel.uiState.value.currentSetIndex)
    }

    @Test
    fun manualFinishRestCancelAndFinishStopTheTimer() {
        val clock = FakeMonotonicClock(1_000L)
        val manual = WorkoutExecutionViewModel(
            snapshot(restSeconds = 60),
            clock = clock,
            tickerEnabled = false,
        )
        manual.start()
        manual.completeCurrentSet()
        manual.finishRest()
        assertEquals(WorkoutRestTimerStatus.Idle, manual.restTimerState.value.status)

        val cancelled = WorkoutExecutionViewModel(
            snapshot(restSeconds = 60),
            clock = clock,
            tickerEnabled = false,
        )
        cancelled.start()
        cancelled.completeCurrentSet()
        cancelled.cancel()
        assertEquals(WorkoutRestTimerStatus.Idle, cancelled.restTimerState.value.status)

        val finished = WorkoutExecutionViewModel(
            snapshot(restSeconds = 60),
            clock = clock,
            tickerEnabled = false,
        )
        finished.start()
        finished.completeCurrentSet()
        finished.finish()
        assertEquals(WorkoutRestTimerStatus.Idle, finished.restTimerState.value.status)
    }
}

private class FakeMonotonicClock(
    var now: Long,
) : MonotonicClock {
    override fun elapsedRealtime(): Long = now
}

private fun snapshot(restSeconds: Int = 0) = WorkoutExecutionSnapshot(
    planId = 1L,
    clientId = 2L,
    startedAt = 3L,
    exercises = listOf(
        WorkoutExecutionExercise(
            localId = "exercise",
            exerciseId = 10L,
            name = "Press",
            sets = listOf(
                WorkoutExecutionSet(1, 8),
                WorkoutExecutionSet(2, 8),
            ),
            restSeconds = restSeconds,
            orderIndex = 1,
        ),
    ),
)
