package com.imanol.gymmanagement.feature.workoutsession.data.remote

import com.imanol.gymmanagement.core.network.model.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface WorkoutSessionApi {
    @POST("api/workout-sessions")
    suspend fun createSession(
        @Body request: WorkoutSessionCreateRequestDto,
    ): ApiResponse<WorkoutSessionResponseDto>

    @GET("api/workout-sessions")
    suspend fun getMySessions(): ApiResponse<List<WorkoutSessionResponseDto>>

    @GET("api/workout-sessions/{id}")
    suspend fun getSession(@Path("id") sessionId: Long): ApiResponse<WorkoutSessionResponseDto>

    @PATCH("api/workout-sessions/{id}")
    suspend fun updateSession(
        @Path("id") sessionId: Long,
        @Body request: WorkoutSessionUpdateRequestDto,
    ): ApiResponse<WorkoutSessionResponseDto>

    @POST("api/workout-sessions/{id}/sets")
    suspend fun addSet(
        @Path("id") sessionId: Long,
        @Body request: WorkoutSessionSetRequestDto,
    ): ApiResponse<WorkoutSessionSetResponseDto>

    @PATCH("api/workout-sessions/{sessionId}/exercises/{exerciseId}")
    suspend fun updateExercise(
        @Path("sessionId") sessionId: Long,
        @Path("exerciseId") exerciseId: Long,
        @Body request: WorkoutSessionExerciseUpdateRequestDto,
    ): ApiResponse<WorkoutSessionExerciseResponseDto>

    @PATCH("api/workout-sessions/{id}/finish")
    suspend fun finishSession(@Path("id") sessionId: Long): ApiResponse<WorkoutSessionResponseDto>

    @PATCH("api/workout-sessions/{id}/cancel")
    suspend fun cancelSession(@Path("id") sessionId: Long): ApiResponse<WorkoutSessionResponseDto>
}
