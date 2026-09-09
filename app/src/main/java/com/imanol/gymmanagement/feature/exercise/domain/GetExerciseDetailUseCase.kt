package com.imanol.gymmanagement.feature.exercise.domain

import javax.inject.Inject

class GetExerciseDetailUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    suspend operator fun invoke(exerciseId: Long): Exercise =
        repository.getExerciseById(exerciseId)
}
