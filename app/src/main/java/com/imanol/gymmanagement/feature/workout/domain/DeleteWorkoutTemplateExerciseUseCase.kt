package com.imanol.gymmanagement.feature.workout.domain

import javax.inject.Inject

class DeleteWorkoutTemplateExerciseUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(templateExerciseId: Long) {
        repository.deleteWorkoutTemplateExercise(templateExerciseId)
    }
}
