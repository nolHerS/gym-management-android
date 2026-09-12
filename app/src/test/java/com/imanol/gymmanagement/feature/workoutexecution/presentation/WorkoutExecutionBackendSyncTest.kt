package com.imanol.gymmanagement.feature.workoutexecution.presentation

import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionExercise
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionSnapshot
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionStatus
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionSet
import com.imanol.gymmanagement.feature.workoutsession.domain.AddWorkoutSessionSetUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.CancelWorkoutSessionUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.FinishWorkoutSessionUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.UpdateWorkoutSessionExerciseUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSession
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionExercise
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionExerciseUpdate
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionRepository
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionSet
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionSetInput
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionStatus
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutExecutionBackendSyncTest {
    @Test
    fun registersEachSetWithSessionExerciseIdAndFinishesOnce() {
        val repository = RecordingSessionRepository()
        val viewModel = viewModel(repository)

        viewModel.start()
        viewModel.completeCurrentSet()
        viewModel.completeCurrentSet()

        assertEquals(listOf(1, 2), repository.sets.map { it.setNumber })
        assertEquals(listOf(99L, 99L), repository.sets.map { it.sessionExerciseId })
        assertEquals(listOf(99L), repository.completedExercises)
        assertEquals(listOf(40L), repository.finishedSessions)
        assertEquals(WorkoutExecutionStatus.Finished, viewModel.uiState.value.status)
    }

    @Test
    fun failedFinishKeepsFinishedStateAndRetryDoesNotCreateAnotherSession() {
        val repository = RecordingSessionRepository().also { it.failFinishOnce = true }
        val viewModel = viewModel(repository, setCount = 1)

        viewModel.start()
        viewModel.completeCurrentSet()

        assertEquals(WorkoutExecutionStatus.Finished, viewModel.uiState.value.status)
        assertTrue(viewModel.syncState.value is WorkoutExecutionSyncState.Error)
        viewModel.retrySynchronization()

        assertEquals(2, repository.finishedSessions.size)
        assertEquals(WorkoutExecutionStatus.Finished, viewModel.uiState.value.status)
    }

    @Test
    fun cancelFailureKeepsCancelledStateAndRetryUsesSameSession() {
        val repository = RecordingSessionRepository().also { it.failCancelOnce = true }
        val viewModel = viewModel(repository)

        viewModel.start()
        viewModel.cancel()
        assertEquals(WorkoutExecutionStatus.Cancelled, viewModel.uiState.value.status)
        assertTrue(viewModel.syncState.value is WorkoutExecutionSyncState.Error)

        viewModel.retrySynchronization()

        assertEquals(listOf(40L, 40L), repository.cancelledSessions)
    }

    private fun viewModel(repository: RecordingSessionRepository, setCount: Int = 2) =
        WorkoutExecutionViewModel(
            snapshot = snapshot(setCount),
            session = session(),
            addSet = AddWorkoutSessionSetUseCase(repository),
            updateExercise = UpdateWorkoutSessionExerciseUseCase(repository),
            finishSession = FinishWorkoutSessionUseCase(repository),
            cancelSession = CancelWorkoutSessionUseCase(repository),
            syncScope = CoroutineScope(Dispatchers.Unconfined),
            tickerEnabled = false,
        )

    private fun snapshot(setCount: Int) = WorkoutExecutionSnapshot(
        planId = 7L,
        clientId = 8L,
        startedAt = 9L,
        exercises = listOf(
            WorkoutExecutionExercise(
                localId = "local",
                exerciseId = 10L,
                name = "Press",
                sets = (1..setCount).map { WorkoutExecutionSet(it, 8) },
                restSeconds = 0,
                orderIndex = 1,
            ),
        ),
    )

    private fun session() = WorkoutSession(
        id = 40L,
        workoutPlanId = 7L,
        clientId = 8L,
        status = WorkoutSessionStatus.IN_PROGRESS,
        startedAt = "2026-09-12T10:00:00",
        finishedAt = null,
        cancelledAt = null,
        durationSeconds = null,
        notes = null,
        version = 0L,
        exercises = listOf(
            WorkoutSessionExercise(
                id = 99L,
                exerciseId = 999L,
                exerciseName = "Press",
                orderIndex = 1,
                plannedSets = 2,
                plannedRepetitions = 8,
                plannedRestSeconds = 0,
                completed = false,
                notes = null,
                sets = emptyList(),
            ),
        ),
    )
}

private class RecordingSessionRepository : WorkoutSessionRepository {
    val sets = mutableListOf<WorkoutSessionSetInput>()
    val completedExercises = mutableListOf<Long>()
    val finishedSessions = mutableListOf<Long>()
    val cancelledSessions = mutableListOf<Long>()
    var failFinishOnce = false
    var failCancelOnce = false

    override suspend fun createSession(workoutPlanId: Long) = error("unused")
    override suspend fun getMySessions() = emptyList<WorkoutSession>()
    override suspend fun getSession(sessionId: Long) = error("unused")
    override suspend fun updateSession(sessionId: Long, update: WorkoutSessionUpdate) = error("unused")
    override suspend fun addSet(sessionId: Long, input: WorkoutSessionSetInput): WorkoutSessionSet {
        sets += input
        return WorkoutSessionSet(1L, input.setNumber, 8, null, null, null, null, true, null)
    }
    override suspend fun updateExercise(
        sessionId: Long,
        exerciseId: Long,
        update: WorkoutSessionExerciseUpdate,
    ): WorkoutSessionExercise {
        completedExercises += exerciseId
        return WorkoutSessionExercise(
            id = exerciseId,
            exerciseId = 999L,
            exerciseName = "Press",
            orderIndex = 1,
            plannedSets = 1,
            plannedRepetitions = 8,
            plannedRestSeconds = 0,
            completed = true,
            notes = null,
            sets = emptyList(),
        )
    }
    override suspend fun finishSession(sessionId: Long): WorkoutSession {
        finishedSessions += sessionId
        if (failFinishOnce) {
            failFinishOnce = false
            error("finish failed")
        }
        return session()
    }
    override suspend fun cancelSession(sessionId: Long): WorkoutSession {
        cancelledSessions += sessionId
        if (failCancelOnce) {
            failCancelOnce = false
            error("cancel failed")
        }
        return session()
    }

    private fun session() = WorkoutSession(
        40L, 7L, 8L, WorkoutSessionStatus.IN_PROGRESS, "now", null, null, null, null, 0L,
        emptyList(),
    )
}
