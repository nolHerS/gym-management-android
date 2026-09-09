package com.imanol.gymmanagement.feature.exercise.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetExercisesByCategoryUseCaseTest {
    @Test
    fun returnsExercisesFromRepository() = runBlocking {
        val exercises = listOf(Exercise(1L, "Bench Press", null, 2L, "Chest", true))
        val useCase = GetExercisesByCategoryUseCase(
            object : ExerciseRepository {
                override suspend fun getExerciseCategories(): List<ExerciseCategory> = emptyList()
                override suspend fun getExercisesByCategory(categoryId: Long): List<Exercise> = exercises
                override suspend fun getExerciseById(exerciseId: Long): Exercise = error("Not used")
            },
        )

        assertEquals(exercises, useCase(2L))
    }
}
