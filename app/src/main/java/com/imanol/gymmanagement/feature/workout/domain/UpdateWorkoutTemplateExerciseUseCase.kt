package com.imanol.gymmanagement.feature.workout.domain

import javax.inject.Inject

class UpdateWorkoutTemplateExerciseUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(
        templateExerciseId: Long,
        exerciseId: Long,
        orderIndex: Int,
        sets: Int,
        repetitions: Int,
        restSeconds: Int,
    ): WorkoutTemplateExercise = repository.updateWorkoutTemplateExercise(
        templateExerciseId,
        exerciseId,
        orderIndex,
        sets,
        repetitions,
        restSeconds,
    )
}
