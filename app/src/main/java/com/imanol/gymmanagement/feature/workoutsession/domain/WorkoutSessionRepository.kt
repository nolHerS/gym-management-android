package com.imanol.gymmanagement.feature.workoutsession.domain

interface WorkoutSessionRepository {
    suspend fun createSession(workoutPlanId: Long): WorkoutSession
    suspend fun getMySessions(): List<WorkoutSession>
    suspend fun getSession(sessionId: Long): WorkoutSession
    suspend fun updateSession(sessionId: Long, update: WorkoutSessionUpdate): WorkoutSession
    suspend fun addSet(sessionId: Long, input: WorkoutSessionSetInput): WorkoutSessionSet
    suspend fun updateExercise(
        sessionId: Long,
        exerciseId: Long,
        update: WorkoutSessionExerciseUpdate,
    ): WorkoutSessionExercise
    suspend fun finishSession(sessionId: Long): WorkoutSession
    suspend fun cancelSession(sessionId: Long): WorkoutSession
}
