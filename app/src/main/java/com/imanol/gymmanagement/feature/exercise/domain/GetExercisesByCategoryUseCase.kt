package com.imanol.gymmanagement.feature.exercise.domain

import javax.inject.Inject

class GetExercisesByCategoryUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    suspend operator fun invoke(categoryId: Long): List<Exercise> =
        repository.getExercisesByCategory(categoryId)
}
