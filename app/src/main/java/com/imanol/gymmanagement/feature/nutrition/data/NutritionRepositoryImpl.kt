package com.imanol.gymmanagement.feature.nutrition.data

import com.imanol.gymmanagement.feature.nutrition.data.remote.NutritionApi
import com.imanol.gymmanagement.core.network.networkCall
import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.feature.nutrition.data.remote.toCreateRemote
import com.imanol.gymmanagement.feature.nutrition.data.remote.toDomain
import com.imanol.gymmanagement.feature.nutrition.data.remote.toRemote
import com.imanol.gymmanagement.feature.nutrition.data.remote.toUpdateRemote
import com.imanol.gymmanagement.feature.nutrition.domain.FoodInput
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionPlanInput
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionRepository
import javax.inject.Inject

class NutritionRepositoryImpl @Inject constructor(
    private val api: NutritionApi,
) : NutritionRepository {
    override suspend fun createPlan(clientId: Long, input: NutritionPlanInput) =
        networkCall { api.createPlan(clientId, input.toCreateRemote()).requireData("nutrition plan").toDomain() }

    override suspend fun getClientPlans(clientId: Long, status: String?) =
        networkCall { api.getClientPlans(clientId, status).requireData("nutrition plans").map { it.toDomain() } }

    override suspend fun getPlan(id: Long) =
        networkCall { api.getPlan(id).requireData("nutrition plan").toDomain() }

    override suspend fun updatePlan(id: Long, input: NutritionPlanInput) =
        networkCall { api.updatePlan(id, input.toUpdateRemote()).requireData("nutrition plan").toDomain() }

    override suspend fun deactivatePlan(id: Long) {
        networkCall { api.deactivatePlan(id) }
    }

    override suspend fun completePlan(id: Long) {
        networkCall { api.completePlan(id) }
    }

    override suspend fun getMyPlans() =
        networkCall { api.getMyPlans().requireData("nutrition plans").map { it.toDomain() } }

    override suspend fun getMyActivePlans() =
        networkCall { api.getMyActivePlans().requireData("active nutrition plans").map { it.toDomain() } }

    override suspend fun getMyPlan(id: Long) =
        networkCall { api.getMyPlan(id).requireData("nutrition plan").toDomain() }

    override suspend fun getFoods() =
        networkCall { api.getFoods().requireData("foods").map { it.toDomain() } }

    override suspend fun getFood(id: Long) =
        networkCall { api.getFood(id).requireData("food").toDomain() }

    override suspend fun createFood(input: FoodInput) =
        networkCall { api.createFood(input.toRemote()).requireData("food").toDomain() }

    override suspend fun updateFood(id: Long, input: FoodInput) =
        networkCall { api.updateFood(id, input.toRemote()).requireData("food").toDomain() }

    override suspend fun activateFood(id: Long) {
        networkCall { api.activateFood(id) }
    }

    override suspend fun deactivateFood(id: Long) {
        networkCall { api.deactivateFood(id) }
    }
}

private fun <T> com.imanol.gymmanagement.core.network.model.ApiResponse<T>.requireData(
    name: String,
): T = data ?: throw AppException.InvalidResponse("$name response did not contain data")
