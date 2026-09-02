package com.imanol.gymmanagement.feature.auth.data.remote

import com.imanol.gymmanagement.core.network.model.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/users/me")
    suspend fun getCurrentUser(): ApiResponse<UserResponse>
}
