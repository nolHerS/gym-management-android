package com.imanol.gymmanagement.feature.workout.domain

import javax.inject.Inject

class ActivateWorkoutTemplateUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(templateId: Long) {
        repository.activateWorkoutTemplate(templateId)
    }
}
