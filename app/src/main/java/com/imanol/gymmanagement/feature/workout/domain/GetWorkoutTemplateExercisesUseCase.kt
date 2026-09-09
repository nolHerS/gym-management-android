package com.imanol.gymmanagement.feature.workout.domain

import javax.inject.Inject

class GetWorkoutTemplateExercisesUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(templateId: Long): List<WorkoutTemplateExercise> =
        repository.getWorkoutTemplateExercises(templateId)
}
