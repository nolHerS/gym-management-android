package com.imanol.gymmanagement.feature.workoutsession.domain

import javax.inject.Inject

class CreateWorkoutSessionUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository,
) {
    suspend operator fun invoke(workoutPlanId: Long): WorkoutSession =
        repository.createSession(workoutPlanId)
}

class GetMyWorkoutSessionsUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository,
) {
    suspend operator fun invoke(): List<WorkoutSession> = repository.getMySessions()
}

class GetWorkoutSessionUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository,
) {
    suspend operator fun invoke(sessionId: Long): WorkoutSession = repository.getSession(sessionId)
}

class UpdateWorkoutSessionUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository,
) {
    suspend operator fun invoke(sessionId: Long, update: WorkoutSessionUpdate): WorkoutSession =
        repository.updateSession(sessionId, update)
}

class AddWorkoutSessionSetUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository,
) {
    suspend operator fun invoke(sessionId: Long, input: WorkoutSessionSetInput): WorkoutSessionSet =
        repository.addSet(sessionId, input)
}

class UpdateWorkoutSessionExerciseUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository,
) {
    suspend operator fun invoke(
        sessionId: Long,
        exerciseId: Long,
        update: WorkoutSessionExerciseUpdate,
    ): WorkoutSessionExercise = repository.updateExercise(sessionId, exerciseId, update)
}

class FinishWorkoutSessionUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository,
) {
    suspend operator fun invoke(sessionId: Long): WorkoutSession = repository.finishSession(sessionId)
}

class CancelWorkoutSessionUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository,
) {
    suspend operator fun invoke(sessionId: Long): WorkoutSession = repository.cancelSession(sessionId)
}
