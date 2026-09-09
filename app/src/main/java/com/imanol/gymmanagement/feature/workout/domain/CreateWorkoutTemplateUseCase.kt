package com.imanol.gymmanagement.feature.workout.domain

import javax.inject.Inject

class CreateWorkoutTemplateUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(name: String, description: String?): WorkoutTemplate =
        repository.createWorkoutTemplate(name, description)
}
