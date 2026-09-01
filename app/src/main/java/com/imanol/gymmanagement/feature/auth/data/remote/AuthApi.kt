package com.imanol.gymmanagement.feature.auth.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: String): UserResponse
}
