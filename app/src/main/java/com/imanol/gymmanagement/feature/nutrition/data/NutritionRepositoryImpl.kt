package com.imanol.gymmanagement.feature.nutrition.data

import com.imanol.gymmanagement.feature.nutrition.data.remote.NutritionApi
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
        api.createPlan(clientId, input.toCreateRemote()).requireData("nutrition plan").toDomain()

    override suspend fun getClientPlans(clientId: Long, status: String?) =
        api.getClientPlans(clientId, status).requireData("nutrition plans").map { it.toDomain() }

    override suspend fun getPlan(id: Long) =
        api.getPlan(id).requireData("nutrition plan").toDomain()

    override suspend fun updatePlan(id: Long, input: NutritionPlanInput) =
        api.updatePlan(id, input.toUpdateRemote()).requireData("nutrition plan").toDomain()

    override suspend fun deactivatePlan(id: Long) {
        api.deactivatePlan(id)
    }

    override suspend fun completePlan(id: Long) {
        api.completePlan(id)
    }

    override suspend fun getMyPlans() =
        api.getMyPlans().requireData("nutrition plans").map { it.toDomain() }

    override suspend fun getMyActivePlans() =
        api.getMyActivePlans().requireData("active nutrition plans").map { it.toDomain() }

    override suspend fun getMyPlan(id: Long) =
        api.getMyPlan(id).requireData("nutrition plan").toDomain()

    override suspend fun getFoods() =
        api.getFoods().requireData("foods").map { it.toDomain() }

    override suspend fun getFood(id: Long) =
        api.getFood(id).requireData("food").toDomain()

    override suspend fun createFood(input: FoodInput) =
        api.createFood(input.toRemote()).requireData("food").toDomain()

    override suspend fun updateFood(id: Long, input: FoodInput) =
        api.updateFood(id, input.toRemote()).requireData("food").toDomain()

    override suspend fun activateFood(id: Long) {
        api.activateFood(id)
    }

    override suspend fun deactivateFood(id: Long) {
        api.deactivateFood(id)
    }
}

private fun <T> com.imanol.gymmanagement.core.network.model.ApiResponse<T>.requireData(
    name: String,
): T = data ?: error("$name response did not contain data")
