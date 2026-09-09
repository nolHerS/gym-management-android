package com.imanol.gymmanagement.feature.workout.domain

interface WorkoutRepository {
    suspend fun getWorkoutTemplates(): List<WorkoutTemplate>

    suspend fun getWorkoutTemplateDetail(templateId: Long): WorkoutTemplateDetail

    suspend fun createWorkoutTemplate(name: String, description: String?): WorkoutTemplate

    suspend fun updateWorkoutTemplate(
        templateId: Long,
        name: String,
        description: String?,
    ): WorkoutTemplate

    suspend fun activateWorkoutTemplate(templateId: Long)

    suspend fun deactivateWorkoutTemplate(templateId: Long)

    suspend fun getWorkoutTemplateExercises(templateId: Long): List<WorkoutTemplateExercise>

    suspend fun addExerciseToWorkoutTemplate(
        templateId: Long,
        exerciseId: Long,
        orderIndex: Int,
        sets: Int,
        repetitions: Int,
        restSeconds: Int,
    ): WorkoutTemplateExercise

    suspend fun updateWorkoutTemplateExercise(
        templateExerciseId: Long,
        exerciseId: Long,
        orderIndex: Int,
        sets: Int,
        repetitions: Int,
        restSeconds: Int,
    ): WorkoutTemplateExercise

    suspend fun deleteWorkoutTemplateExercise(templateExerciseId: Long)
}
