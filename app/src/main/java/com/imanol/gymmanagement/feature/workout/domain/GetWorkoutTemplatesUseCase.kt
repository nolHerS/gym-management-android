package com.imanol.gymmanagement.feature.workout.domain

import javax.inject.Inject

class GetWorkoutTemplatesUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(): List<WorkoutTemplate> = repository.getWorkoutTemplates()
}
