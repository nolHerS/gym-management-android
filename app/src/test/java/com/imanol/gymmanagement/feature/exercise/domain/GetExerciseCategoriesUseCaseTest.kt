package com.imanol.gymmanagement.feature.exercise.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetExerciseCategoriesUseCaseTest {
    @Test
    fun returnsCategoriesFromRepository() = runBlocking {
        val categories = listOf(
            ExerciseCategory(
                id = 1L,
                name = "Chest",
                active = true,
                createdAt = null,
                updatedAt = null,
            ),
        )
        val useCase = GetExerciseCategoriesUseCase(
            object : ExerciseRepository {
                override suspend fun getExerciseCategories(): List<ExerciseCategory> = categories
                override suspend fun getExercisesByCategory(categoryId: Long): List<Exercise> = emptyList()
                override suspend fun getExerciseById(exerciseId: Long): Exercise =
                    error("Not used")
            },
        )

        assertEquals(categories, useCase())
    }
}
