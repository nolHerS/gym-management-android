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

class AddWorkoutPlanDayUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository,
) {
    suspend operator fun invoke(
        planId: Long,
        request: WorkoutPlanDayRequest,
    ) = repository.addDay(planId, request)
}

class DeleteWorkoutPlanDayUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository,
) {
    suspend operator fun invoke(planId: Long, dayOfWeek: Int) {
        repository.deleteDay(planId, dayOfWeek)
    }
}

class AddWorkoutPlanExerciseUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository,
) {
    suspend operator fun invoke(
        planId: Long,
        dayOfWeek: Int,
        request: WorkoutPlanExerciseRequest,
    ) = repository.addExercise(planId, dayOfWeek, request)
}

class UpdateWorkoutPlanExerciseUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository,
) {
    suspend operator fun invoke(
        exerciseId: Long,
        request: WorkoutPlanExerciseRequest,
    ) = repository.updateExercise(exerciseId, request)
}

class DeleteWorkoutPlanExerciseUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository,
) {
    suspend operator fun invoke(exerciseId: Long) {
        repository.deleteExercise(exerciseId)
    }
}

class DeactivateWorkoutPlanUseCase @Inject constructor(private val repository: WorkoutPlanRepository) {
    suspend operator fun invoke(id: Long) = repository.deactivate(id)
}
class CompleteWorkoutPlanUseCase @Inject constructor(private val repository: WorkoutPlanRepository) {
    suspend operator fun invoke(id: Long) = repository.complete(id)
}
