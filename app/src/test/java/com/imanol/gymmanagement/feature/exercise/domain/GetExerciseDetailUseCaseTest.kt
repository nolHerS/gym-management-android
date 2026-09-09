package com.imanol.gymmanagement.feature.exercise.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetExerciseDetailUseCaseTest {
    @Test
    fun returnsExerciseFromRepository() = runBlocking {
        val exercise = Exercise(1L, "Bench Press", null, 2L, "Chest", true)
        val useCase = GetExerciseDetailUseCase(
            object : ExerciseRepository {
                override suspend fun getExerciseCategories(): List<ExerciseCategory> = emptyList()
                override suspend fun getExercisesByCategory(categoryId: Long): List<Exercise> =
                    emptyList()
                override suspend fun getExerciseById(exerciseId: Long): Exercise = exercise
            },
        )

        assertEquals(exercise, useCase(1L))
    }
}
