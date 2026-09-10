package com.imanol.gymmanagement.feature.nutrition.data

import com.imanol.gymmanagement.core.network.model.ApiResponse
import com.imanol.gymmanagement.feature.nutrition.data.remote.CreateNutritionPlanRequest
import com.imanol.gymmanagement.feature.nutrition.data.remote.FoodRequest
import com.imanol.gymmanagement.feature.nutrition.data.remote.FoodResponse
import com.imanol.gymmanagement.feature.nutrition.data.remote.NutritionApi
import com.imanol.gymmanagement.feature.nutrition.data.remote.NutritionPlanResponse
import com.imanol.gymmanagement.feature.nutrition.data.remote.UpdateNutritionPlanRequest
import com.imanol.gymmanagement.feature.nutrition.sampleFood
import com.imanol.gymmanagement.feature.nutrition.samplePlan
import com.imanol.gymmanagement.feature.nutrition.domain.FoodInput
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionPlanInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionRepositoryImplTest {
    @Test
    fun delegatesAllPlanAndFoodOperationsToApi() = runBlocking {
        val api = RecordingNutritionApi()
        val repository = NutritionRepositoryImpl(api)
        val planInput = NutritionPlanInput(
            name = "Plan",
            description = null,
            startDate = "2026-09-07",
            endDate = null,
            meals = emptyList(),
        )
        val foodInput = FoodInput("Avena", null, null, null, null, null, null, null)

        assertEquals(samplePlan(), repository.createPlan(2L, planInput))
        assertEquals(samplePlan(), repository.updatePlan(10L, planInput.copy(status = "ACTIVE")))
        assertEquals(listOf(samplePlan()), repository.getClientPlans(2L, "ACTIVE"))
        assertEquals(samplePlan(), repository.getPlan(10L))
        repository.deactivatePlan(10L)
        repository.completePlan(10L)
        assertEquals(listOf(samplePlan()), repository.getMyPlans())
        assertEquals(listOf(samplePlan()), repository.getMyActivePlans())
        assertEquals(samplePlan(), repository.getMyPlan(10L))
        assertEquals(listOf(sampleFood()), repository.getFoods())
        assertEquals(sampleFood(), repository.getFood(1L))
        assertEquals(sampleFood(), repository.createFood(foodInput))
        assertEquals(sampleFood(), repository.updateFood(1L, foodInput))
        repository.activateFood(1L)
        repository.deactivateFood(1L)

        assertEquals(2L, api.createdClientId)
        assertEquals(10L, api.updatedPlanId)
        assertEquals("ACTIVE", api.requestedStatus)
        assertEquals(10L, api.deactivatedPlanId)
        assertEquals(10L, api.completedPlanId)
        assertEquals(1L, api.updatedFoodId)
        assertEquals(1L, api.activatedFoodId)
        assertEquals(1L, api.deactivatedFoodId)
        assertTrue(api.myPlansCalled)
        assertTrue(api.myActivePlansCalled)
    }
}

private class RecordingNutritionApi : NutritionApi {
    private val planResponse = NutritionPlanResponse(
        id = 10L,
        clientId = 2L,
        trainerId = 1L,
        name = "Definición",
        description = "Inicial",
        startDate = "2026-09-07",
        endDate = null,
        status = "ACTIVE",
        meals = emptyList(),
        createdAt = "2026-09-09T16:55:00",
        updatedAt = "2026-09-09T16:55:00",
    )
    private val foodResponse = FoodResponse(
        id = 1L,
        name = "Avena",
        description = "Integral",
        calories = "389",
        protein = "16.9",
        carbohydrates = "66.3",
        fats = "6.9",
        servingSize = "100",
        servingUnit = "g",
        active = true,
        createdAt = "2026-09-09T16:55:00",
        updatedAt = "2026-09-09T16:55:00",
    )

    var createdClientId: Long? = null
    var updatedPlanId: Long? = null
    var requestedStatus: String? = null
    var deactivatedPlanId: Long? = null
    var completedPlanId: Long? = null
    var updatedFoodId: Long? = null
    var activatedFoodId: Long? = null
    var deactivatedFoodId: Long? = null
    var myPlansCalled = false
    var myActivePlansCalled = false

    override suspend fun createPlan(
        clientId: Long,
        request: CreateNutritionPlanRequest,
    ): ApiResponse<NutritionPlanResponse> {
        createdClientId = clientId
        return response(planResponse)
    }

    override suspend fun getClientPlans(
        clientId: Long,
        status: String?,
    ): ApiResponse<List<NutritionPlanResponse>> {
        requestedStatus = status
        return response(listOf(planResponse))
    }

    override suspend fun getPlan(id: Long) = response(planResponse)

    override suspend fun updatePlan(
        id: Long,
        request: UpdateNutritionPlanRequest,
    ): ApiResponse<NutritionPlanResponse> {
        updatedPlanId = id
        return response(planResponse)
    }

    override suspend fun deactivatePlan(id: Long): ApiResponse<Unit> {
        deactivatedPlanId = id
        return response(null)
    }

    override suspend fun completePlan(id: Long): ApiResponse<Unit> {
        completedPlanId = id
        return response(null)
    }

    override suspend fun getMyPlans(): ApiResponse<List<NutritionPlanResponse>> {
        myPlansCalled = true
        return response(listOf(planResponse))
    }

    override suspend fun getMyActivePlans(): ApiResponse<List<NutritionPlanResponse>> {
        myActivePlansCalled = true
        return response(listOf(planResponse))
    }

    override suspend fun getMyPlan(id: Long) = response(planResponse)
    override suspend fun getFoods() = response(listOf(foodResponse))
    override suspend fun getFood(id: Long) = response(foodResponse)
    override suspend fun createFood(request: FoodRequest) = response(foodResponse)

    override suspend fun updateFood(
        id: Long,
        request: FoodRequest,
    ): ApiResponse<FoodResponse> {
        updatedFoodId = id
        return response(foodResponse)
    }

    override suspend fun activateFood(id: Long): ApiResponse<Unit> {
        activatedFoodId = id
        return response(null)
    }

    override suspend fun deactivateFood(id: Long): ApiResponse<Unit> {
        deactivatedFoodId = id
        return response(null)
    }

    private fun <T> response(data: T?) = ApiResponse(
        timestamp = "2026-09-09T16:55:00",
        status = 200,
        error = null,
        message = "OK",
        data = data,
    )
}
