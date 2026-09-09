package com.imanol.gymmanagement.feature.workout.domain

import javax.inject.Inject

class AddExerciseToWorkoutTemplateUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(
        templateId: Long,
        exerciseId: Long,
        orderIndex: Int,
        sets: Int,
        repetitions: Int,
        restSeconds: Int,
    ): WorkoutTemplateExercise = repository.addExerciseToWorkoutTemplate(
        templateId,
        exerciseId,
        orderIndex,
        sets,
        repetitions,
        restSeconds,
    )
}
