package com.imanol.gymmanagement.feature.workout.domain

import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetWorkoutTemplateDetailUseCaseTest {
    @Test
    fun returnsTemplateDetailFromRepository() = runBlocking {
        val detail = WorkoutTemplateDetail(
            WorkoutTemplate(1L, "Strength", null, true),
            listOf(
                WorkoutTemplateExercise(
                    4L,
                    Exercise(2L, "Squat", null, 1L, "Legs", true),
                    1,
                    3,
                    10,
                    60,
                ),
            ),
        )
        val useCase = GetWorkoutTemplateDetailUseCase(
            object : WorkoutRepository by EmptyWorkoutRepository() {
                override suspend fun getWorkoutTemplateDetail(
                    templateId: Long,
                ): WorkoutTemplateDetail = detail
            },
        )

        assertEquals(detail, useCase(1L))
    }
}

internal open class EmptyWorkoutRepository : WorkoutRepository {
    override suspend fun getWorkoutTemplates(): List<WorkoutTemplate> = emptyList()
    override suspend fun getWorkoutTemplateDetail(templateId: Long): WorkoutTemplateDetail =
        error("Not used")
    override suspend fun createWorkoutTemplate(name: String, description: String?): WorkoutTemplate =
        error("Not used")
    override suspend fun updateWorkoutTemplate(
        templateId: Long,
        name: String,
        description: String?,
    ): WorkoutTemplate = error("Not used")
    override suspend fun activateWorkoutTemplate(templateId: Long) = Unit
    override suspend fun deactivateWorkoutTemplate(templateId: Long) = Unit
    override suspend fun getWorkoutTemplateExercises(
        templateId: Long,
    ): List<WorkoutTemplateExercise> = emptyList()
    override suspend fun addExerciseToWorkoutTemplate(
        templateId: Long,
        exerciseId: Long,
        orderIndex: Int,
        sets: Int,
        repetitions: Int,
        restSeconds: Int,
    ): WorkoutTemplateExercise = error("Not used")
    override suspend fun updateWorkoutTemplateExercise(
        templateExerciseId: Long,
        exerciseId: Long,
        orderIndex: Int,
        sets: Int,
        repetitions: Int,
        restSeconds: Int,
    ): WorkoutTemplateExercise = error("Not used")
    override suspend fun deleteWorkoutTemplateExercise(templateExerciseId: Long) = Unit
}
