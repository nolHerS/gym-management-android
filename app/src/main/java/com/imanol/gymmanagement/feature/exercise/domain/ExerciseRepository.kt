package com.imanol.gymmanagement.feature.exercise.domain

interface ExerciseRepository {
    suspend fun getExerciseCategories(): List<ExerciseCategory>

    suspend fun getExercisesByCategory(categoryId: Long): List<Exercise>

    suspend fun getExerciseById(exerciseId: Long): Exercise
}
