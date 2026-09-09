package com.imanol.gymmanagement.feature.exercise.data.remote

import com.imanol.gymmanagement.core.network.model.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ExerciseApi {
    @GET("api/exercise-categories")
    suspend fun getExerciseCategories(): ApiResponse<List<ExerciseCategoryResponse>>

    @GET("api/exercises/category/{categoryId}")
    suspend fun getExercisesByCategory(
        @Path("categoryId") categoryId: Long,
    ): ApiResponse<List<ExerciseResponse>>

    @GET("api/exercises/{id}")
    suspend fun getExerciseById(
        @Path("id") exerciseId: Long,
    ): ApiResponse<ExerciseResponse>
}
