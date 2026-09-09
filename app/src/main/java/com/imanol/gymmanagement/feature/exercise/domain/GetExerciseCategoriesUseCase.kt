package com.imanol.gymmanagement.feature.exercise.domain

import javax.inject.Inject

class GetExerciseCategoriesUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    suspend operator fun invoke(): List<ExerciseCategory> =
        repository.getExerciseCategories()
}
