package com.imanol.gymmanagement.feature.workout.data

import com.imanol.gymmanagement.feature.workout.data.remote.WorkoutApi
import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.core.network.networkCall
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
    override suspend fun getWorkoutTemplates(): List<WorkoutTemplate> = networkCall {
        workoutApi.getWorkoutTemplates().data
            ?.map { it.toDomain() }
            ?: throw AppException.InvalidResponse("Workout templates response did not contain data")
    }

    override suspend fun getWorkoutTemplateDetail(templateId: Long): WorkoutTemplateDetail {
        val template = networkCall {
            workoutApi.getWorkoutTemplate(templateId).data?.toDomain()
                ?: throw AppException.InvalidResponse("Workout template response did not contain data")
        }
        val exercises = getWorkoutTemplateExercises(templateId)
        return WorkoutTemplateDetail(template, exercises)
    }

    override suspend fun createWorkoutTemplate(
        name: String,
        description: String?,
    ): WorkoutTemplate =
        networkCall {
            workoutApi.createWorkoutTemplate(
                WorkoutTemplateRequest(name = name, description = description),
            ).data?.toDomain()
                ?: throw AppException.InvalidResponse("Created workout template response did not contain data")
        }

    override suspend fun updateWorkoutTemplate(
        templateId: Long,
        name: String,
        description: String?,
    ): WorkoutTemplate =
        networkCall {
            workoutApi.updateWorkoutTemplate(
                templateId,
                WorkoutTemplateRequest(name = name, description = description),
            ).data?.toDomain()
                ?: throw AppException.InvalidResponse("Updated workout template response did not contain data")
        }

    override suspend fun activateWorkoutTemplate(templateId: Long) {
        networkCall { workoutApi.activateWorkoutTemplate(templateId) }
    }

    override suspend fun deactivateWorkoutTemplate(templateId: Long) {
        networkCall { workoutApi.deactivateWorkoutTemplate(templateId) }
    }

    override suspend fun getWorkoutTemplateExercises(
        templateId: Long,
    ): List<WorkoutTemplateExercise> = networkCall {
        workoutApi.getWorkoutTemplateExercises(templateId).data
            ?.map { it.toDomain() }
            ?: throw AppException.InvalidResponse("Workout template exercises response did not contain data")
    }

    override suspend fun addExerciseToWorkoutTemplate(
        templateId: Long,
        exerciseId: Long,
        orderIndex: Int,
        sets: Int,
        repetitions: Int,
        restSeconds: Int,
    ): WorkoutTemplateExercise =
        networkCall {
            workoutApi.addExerciseToWorkoutTemplate(
                templateId,
                WorkoutTemplateExerciseRequest(
                    exerciseId,
                    orderIndex,
                    sets,
                    repetitions,
                    restSeconds,
                ),
            ).data?.toDomain()
                ?: throw AppException.InvalidResponse("Added workout exercise response did not contain data")
        }

    override suspend fun updateWorkoutTemplateExercise(
        templateExerciseId: Long,
        exerciseId: Long,
        orderIndex: Int,
        sets: Int,
        repetitions: Int,
        restSeconds: Int,
    ): WorkoutTemplateExercise =
        networkCall {
            workoutApi.updateWorkoutTemplateExercise(
                templateExerciseId,
                WorkoutTemplateExerciseRequest(
                    exerciseId,
                    orderIndex,
                    sets,
                    repetitions,
                    restSeconds,
                ),
            ).data?.toDomain()
                ?: throw AppException.InvalidResponse("Updated workout exercise response did not contain data")
        }

    override suspend fun deleteWorkoutTemplateExercise(templateExerciseId: Long) {
        networkCall { workoutApi.deleteWorkoutTemplateExercise(templateExerciseId) }
    }
}
