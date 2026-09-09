package com.imanol.gymmanagement.feature.client.data.remote

import com.imanol.gymmanagement.core.network.model.ApiResponse
import com.imanol.gymmanagement.feature.auth.data.remote.UserResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ClientApi {
    @GET("api/trainer-clients/trainer/{trainerId}")
    suspend fun getTrainerClients(
        @Path("trainerId") trainerId: Long,
    ): ApiResponse<List<TrainerClientResponse>>

    @GET("api/users/{clientId}")
    suspend fun getClientById(
        @Path("clientId") clientId: Long,
    ): ApiResponse<UserResponse>
}
