package com.imanol.gymmanagement.feature.workout.data.remote

import com.imanol.gymmanagement.core.network.model.ApiResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface WorkoutApi {
    @GET("api/workout-templates")
    suspend fun getWorkoutTemplates(): ApiResponse<List<WorkoutTemplateResponse>>

    @GET("api/workout-templates/{id}")
    suspend fun getWorkoutTemplate(
        @Path("id") templateId: Long,
    ): ApiResponse<WorkoutTemplateResponse>

    @POST("api/workout-templates")
    suspend fun createWorkoutTemplate(
        @Body request: WorkoutTemplateRequest,
    ): ApiResponse<WorkoutTemplateResponse>

    @PUT("api/workout-templates/{id}")
    suspend fun updateWorkoutTemplate(
        @Path("id") templateId: Long,
        @Body request: WorkoutTemplateRequest,
    ): ApiResponse<WorkoutTemplateResponse>

    @PATCH("api/workout-templates/{id}/activate")
    suspend fun activateWorkoutTemplate(
        @Path("id") templateId: Long,
    ): ApiResponse<Unit>

    @PATCH("api/workout-templates/{id}/deactivate")
    suspend fun deactivateWorkoutTemplate(
        @Path("id") templateId: Long,
    ): ApiResponse<Unit>

    @GET("api/workout-templates/{workoutTemplateId}/exercises")
    suspend fun getWorkoutTemplateExercises(
        @Path("workoutTemplateId") templateId: Long,
    ): ApiResponse<List<WorkoutTemplateExerciseResponse>>

    @POST("api/workout-templates/{workoutTemplateId}/exercises")
    suspend fun addExerciseToWorkoutTemplate(
        @Path("workoutTemplateId") templateId: Long,
        @Body request: WorkoutTemplateExerciseRequest,
    ): ApiResponse<WorkoutTemplateExerciseResponse>

    @PUT("api/workout-template-exercises/{id}")
    suspend fun updateWorkoutTemplateExercise(
        @Path("id") templateExerciseId: Long,
        @Body request: WorkoutTemplateExerciseRequest,
    ): ApiResponse<WorkoutTemplateExerciseResponse>

    @DELETE("api/workout-template-exercises/{id}")
    suspend fun deleteWorkoutTemplateExercise(
        @Path("id") templateExerciseId: Long,
    ): ApiResponse<Unit>
}
