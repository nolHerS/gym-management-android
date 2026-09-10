package com.imanol.gymmanagement.feature.workoutplan.domain

import javax.inject.Inject

class GetMyWorkoutPlanUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository,
) {
    suspend operator fun invoke(): List<WorkoutPlan> = repository.getMine()
}

class GetMyWorkoutWeekUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository,
) {
    suspend operator fun invoke(weekStart: String): List<WorkoutPlan> =
        repository.getMyWeek(weekStart)
}

class GetClientWorkoutPlansUseCase @Inject constructor(private val repository: WorkoutPlanRepository) {
    suspend operator fun invoke(clientId: Long) = repository.getForClient(clientId)
}
class GetWorkoutPlanDetailUseCase @Inject constructor(private val repository: WorkoutPlanRepository) {
    suspend operator fun invoke(id: Long) = repository.get(id)
}
class CreateWorkoutPlanUseCase @Inject constructor(private val repository: WorkoutPlanRepository) {
    suspend operator fun invoke(clientId: Long, request: CreateWorkoutPlanRequest): WorkoutPlan {
        request.validationError()?.let { error -> throw IllegalArgumentException(error) }
        return repository.create(clientId, request)
    }
}
class UpdateWorkoutPlanUseCase @Inject constructor(private val repository: WorkoutPlanRepository) {
    suspend operator fun invoke(id: Long, request: UpdateWorkoutPlanRequest) = repository.update(id, request)
}
class DeactivateWorkoutPlanUseCase @Inject constructor(private val repository: WorkoutPlanRepository) {
    suspend operator fun invoke(id: Long) = repository.deactivate(id)
}
class CompleteWorkoutPlanUseCase @Inject constructor(private val repository: WorkoutPlanRepository) {
    suspend operator fun invoke(id: Long) = repository.complete(id)
}
