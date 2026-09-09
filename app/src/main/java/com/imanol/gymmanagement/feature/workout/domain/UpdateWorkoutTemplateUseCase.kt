package com.imanol.gymmanagement.feature.workout.domain

import javax.inject.Inject

class UpdateWorkoutTemplateUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(
        templateId: Long,
        name: String,
        description: String?,
    ): WorkoutTemplate = repository.updateWorkoutTemplate(templateId, name, description)
}
