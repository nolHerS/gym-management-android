package com.imanol.gymmanagement.feature.exercise.data

import com.imanol.gymmanagement.feature.exercise.data.remote.ExerciseApi
import com.imanol.gymmanagement.feature.exercise.data.remote.toDomain
import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseCategory
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseRepository
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseApi: ExerciseApi,
) : ExerciseRepository {
    override suspend fun getExerciseCategories(): List<ExerciseCategory> =
        exerciseApi.getExerciseCategories().data
            ?.map { it.toDomain() }
            ?: error("Exercise categories response did not contain data")

    override suspend fun getExercisesByCategory(categoryId: Long): List<Exercise> =
        exerciseApi.getExercisesByCategory(categoryId).data
            ?.map { it.toDomain() }
            ?: error("Exercises response did not contain data")

    override suspend fun getExerciseById(exerciseId: Long): Exercise =
        exerciseApi.getExerciseById(exerciseId).data?.toDomain()
            ?: error("Exercise response did not contain data")
}
