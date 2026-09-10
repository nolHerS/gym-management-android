package com.imanol.gymmanagement.feature.nutrition.data.remote

import com.imanol.gymmanagement.core.network.model.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NutritionApi {
    @POST("api/nutrition-plans/clients/{clientId}")
    suspend fun createPlan(
        @Path("clientId") clientId: Long,
        @Body request: CreateNutritionPlanRequest,
    ): ApiResponse<NutritionPlanResponse>

    @GET("api/nutrition-plans/clients/{clientId}")
    suspend fun getClientPlans(
        @Path("clientId") clientId: Long,
        @Query("status") status: String? = null,
    ): ApiResponse<List<NutritionPlanResponse>>

    @GET("api/nutrition-plans/{id}")
    suspend fun getPlan(@Path("id") id: Long): ApiResponse<NutritionPlanResponse>

    @PUT("api/nutrition-plans/{id}")
    suspend fun updatePlan(
        @Path("id") id: Long,
        @Body request: UpdateNutritionPlanRequest,
    ): ApiResponse<NutritionPlanResponse>

    @PATCH("api/nutrition-plans/{id}/deactivate")
    suspend fun deactivatePlan(@Path("id") id: Long): ApiResponse<Unit>

    @PATCH("api/nutrition-plans/{id}/complete")
    suspend fun completePlan(@Path("id") id: Long): ApiResponse<Unit>

    @GET("api/nutrition-plans/me")
    suspend fun getMyPlans(): ApiResponse<List<NutritionPlanResponse>>

    @GET("api/nutrition-plans/me/active")
    suspend fun getMyActivePlans(): ApiResponse<List<NutritionPlanResponse>>

    @GET("api/nutrition-plans/me/{id}")
    suspend fun getMyPlan(@Path("id") id: Long): ApiResponse<NutritionPlanResponse>

    @GET("api/foods")
    suspend fun getFoods(): ApiResponse<List<FoodResponse>>

    @GET("api/foods/{id}")
    suspend fun getFood(@Path("id") id: Long): ApiResponse<FoodResponse>

    @POST("api/foods")
    suspend fun createFood(@Body request: FoodRequest): ApiResponse<FoodResponse>

    @PUT("api/foods/{id}")
    suspend fun updateFood(
        @Path("id") id: Long,
        @Body request: FoodRequest,
    ): ApiResponse<FoodResponse>

    @PATCH("api/foods/{id}/activate")
    suspend fun activateFood(@Path("id") id: Long): ApiResponse<Unit>

    @PATCH("api/foods/{id}/deactivate")
    suspend fun deactivateFood(@Path("id") id: Long): ApiResponse<Unit>
}
