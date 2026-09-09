package com.imanol.gymmanagement.feature.workout.domain

import javax.inject.Inject

class DeactivateWorkoutTemplateUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(templateId: Long) {
        repository.deactivateWorkoutTemplate(templateId)
    }
}
