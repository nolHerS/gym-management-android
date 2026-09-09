package com.imanol.gymmanagement.feature.workout.domain

import javax.inject.Inject

class GetWorkoutTemplateDetailUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(templateId: Long): WorkoutTemplateDetail =
        repository.getWorkoutTemplateDetail(templateId)
}
