package com.imanol.gymmanagement.feature.workout.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetWorkoutTemplatesUseCaseTest {
    @Test
    fun returnsTemplatesFromRepository() = runBlocking {
        val templates = listOf(WorkoutTemplate(1L, "Strength", null, true))
        val useCase = GetWorkoutTemplatesUseCase(
            object : WorkoutRepository by EmptyWorkoutRepository() {
                override suspend fun getWorkoutTemplates(): List<WorkoutTemplate> = templates
            },
        )

        assertEquals(templates, useCase())
    }
}
