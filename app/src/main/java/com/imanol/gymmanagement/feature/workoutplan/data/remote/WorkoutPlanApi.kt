package com.imanol.gymmanagement.feature.workoutplan.data.remote

import com.imanol.gymmanagement.core.network.model.ApiResponse
import retrofit2.http.*

interface WorkoutPlanApi {
    @GET("api/workout-plans/me")
    suspend fun getMine(): ApiResponse<List<WorkoutPlanResponse>>

    @GET("api/workout-plans/me/week")
    suspend fun getMyWeek(@Query("weekStart") weekStart: String): ApiResponse<List<WorkoutPlanResponse>>

    @POST("api/workout-plans/clients/{clientId}")
    suspend fun create(@Path("clientId") clientId: Long, @Body request: WorkoutPlanRequest): ApiResponse<WorkoutPlanResponse>
    @GET("api/workout-plans/clients/{clientId}")
    suspend fun getForClient(@Path("clientId") clientId: Long): ApiResponse<List<WorkoutPlanResponse>>
    @GET("api/workout-plans/{id}")
    suspend fun get(@Path("id") id: Long): ApiResponse<WorkoutPlanResponse>
    @PATCH("api/workout-plans/{id}")
    suspend fun update(@Path("id") id: Long, @Body request: RemoteUpdateWorkoutPlanRequest): ApiResponse<WorkoutPlanResponse>
    @POST("api/workout-plans/{planId}/days")
    suspend fun addDay(@Path("planId") planId: Long, @Body request: DayRequest): ApiResponse<WorkoutPlanResponse>
    @DELETE("api/workout-plans/{planId}/days/{dayOfWeek}")
    suspend fun deleteDay(@Path("planId") planId: Long, @Path("dayOfWeek") day: Int): ApiResponse<Unit>
    @POST("api/workout-plans/{planId}/days/{dayOfWeek}/exercises")
    suspend fun addExercise(@Path("planId") planId: Long, @Path("dayOfWeek") day: Int, @Body request: ExerciseRequest): ApiResponse<WorkoutPlanResponse>
    @PUT("api/workout-plans/exercises/{id}")
    suspend fun updateExercise(@Path("id") id: Long, @Body request: ExerciseRequest): ApiResponse<WorkoutPlanResponse>
    @DELETE("api/workout-plans/exercises/{id}")
    suspend fun deleteExercise(@Path("id") id: Long): ApiResponse<Unit>
    @PATCH("api/workout-plans/{id}/deactivate")
    suspend fun deactivate(@Path("id") id: Long): ApiResponse<Unit>
    @PATCH("api/workout-plans/{id}/complete")
    suspend fun complete(@Path("id") id: Long): ApiResponse<Unit>
}
