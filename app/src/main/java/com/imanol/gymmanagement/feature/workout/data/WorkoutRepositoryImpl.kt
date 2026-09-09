package com.imanol.gymmanagement.feature.workout.data

import com.imanol.gymmanagement.feature.workout.data.remote.WorkoutApi
import com.imanol.gymmanagement.feature.workout.data.remote.WorkoutTemplateExerciseRequest
import com.imanol.gymmanagement.feature.workout.data.remote.WorkoutTemplateRequest
import com.imanol.gymmanagement.feature.workout.data.remote.toDomain
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplate
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplateDetail
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplateExercise
import com.imanol.gymmanagement.feature.workout.domain.WorkoutRepository
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val workoutApi: WorkoutApi,
) : WorkoutRepository {
    override suspend fun getWorkoutTemplates(): List<WorkoutTemplate> =
        workoutApi.getWorkoutTemplates().data
            ?.map { it.toDomain() }
            ?: error("Workout templates response did not contain data")

    override suspend fun getWorkoutTemplateDetail(templateId: Long): WorkoutTemplateDetail {
        val template = workoutApi.getWorkoutTemplate(templateId).data?.toDomain()
            ?: error("Workout template response did not contain data")
        val exercises = getWorkoutTemplateExercises(templateId)
        return WorkoutTemplateDetail(template, exercises)
    }

    override suspend fun createWorkoutTemplate(
        name: String,
        description: String?,
    ): WorkoutTemplate =
        workoutApi.createWorkoutTemplate(
            WorkoutTemplateRequest(name = name, description = description),
        ).data?.toDomain() ?: error("Created workout template response did not contain data")

    override suspend fun updateWorkoutTemplate(
        templateId: Long,
        name: String,
        description: String?,
    ): WorkoutTemplate =
        workoutApi.updateWorkoutTemplate(
            templateId,
            WorkoutTemplateRequest(name = name, description = description),
        ).data?.toDomain() ?: error("Updated workout template response did not contain data")

    override suspend fun activateWorkoutTemplate(templateId: Long) {
        workoutApi.activateWorkoutTemplate(templateId)
    }

    override suspend fun deactivateWorkoutTemplate(templateId: Long) {
        workoutApi.deactivateWorkoutTemplate(templateId)
    }

    override suspend fun getWorkoutTemplateExercises(
        templateId: Long,
    ): List<WorkoutTemplateExercise> =
        workoutApi.getWorkoutTemplateExercises(templateId).data
            ?.map { it.toDomain() }
            ?: error("Workout template exercises response did not contain data")

    override suspend fun addExerciseToWorkoutTemplate(
        templateId: Long,
        exerciseId: Long,
        orderIndex: Int,
        sets: Int,
        repetitions: Int,
        restSeconds: Int,
    ): WorkoutTemplateExercise =
        workoutApi.addExerciseToWorkoutTemplate(
            templateId,
            WorkoutTemplateExerciseRequest(
                exerciseId,
                orderIndex,
                sets,
                repetitions,
                restSeconds,
            ),
        ).data?.toDomain() ?: error("Added workout exercise response did not contain data")

    override suspend fun updateWorkoutTemplateExercise(
        templateExerciseId: Long,
        exerciseId: Long,
        orderIndex: Int,
        sets: Int,
        repetitions: Int,
        restSeconds: Int,
    ): WorkoutTemplateExercise =
        workoutApi.updateWorkoutTemplateExercise(
            templateExerciseId,
            WorkoutTemplateExerciseRequest(
                exerciseId,
                orderIndex,
                sets,
                repetitions,
                restSeconds,
            ),
        ).data?.toDomain() ?: error("Updated workout exercise response did not contain data")

    override suspend fun deleteWorkoutTemplateExercise(templateExerciseId: Long) {
        workoutApi.deleteWorkoutTemplateExercise(templateExerciseId)
    }
}
