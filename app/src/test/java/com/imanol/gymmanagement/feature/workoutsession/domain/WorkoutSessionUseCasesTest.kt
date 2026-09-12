package com.imanol.gymmanagement.feature.workoutsession.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSessionUseCasesTest {
    @Test
    fun useCasesDelegateToRepository() = runBlocking {
        val repository = RecordingRepository()
        assertEquals(9L, CreateWorkoutSessionUseCase(repository)(2L).id)
        assertEquals(1, GetMyWorkoutSessionsUseCase(repository)().size)
        assertEquals(9L, GetWorkoutSessionUseCase(repository)(9L).id)
        assertEquals(9L, FinishWorkoutSessionUseCase(repository)(9L).id)
        assertEquals(9L, CancelWorkoutSessionUseCase(repository)(9L).id)
        assertEquals(2L, repository.createdPlanId)
    }
}

private class RecordingRepository : WorkoutSessionRepository {
    var createdPlanId: Long? = null
    private val session = WorkoutSession(9L, 2L, 3L, WorkoutSessionStatus.IN_PROGRESS, "now", null, null, null, null, 0L, emptyList())

    override suspend fun createSession(workoutPlanId: Long): WorkoutSession {
        createdPlanId = workoutPlanId
        return session
    }
    override suspend fun getMySessions() = listOf(session)
    override suspend fun getSession(sessionId: Long) = session
    override suspend fun updateSession(sessionId: Long, update: WorkoutSessionUpdate) = session
    override suspend fun addSet(sessionId: Long, input: WorkoutSessionSetInput) = WorkoutSessionSet(1L, 1, 1, null, null, null, null, false, null)
    override suspend fun updateExercise(sessionId: Long, exerciseId: Long, update: WorkoutSessionExerciseUpdate) =
        WorkoutSessionExercise(1L, 1L, "Exercise", 1, 1, 1, 1, false, null, emptyList())
    override suspend fun finishSession(sessionId: Long) = session
    override suspend fun cancelSession(sessionId: Long) = session
}
