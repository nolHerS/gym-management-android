package com.imanol.gymmanagement.feature.nutrition.domain

interface NutritionRepository {
    suspend fun createPlan(clientId: Long, input: NutritionPlanInput): NutritionPlan
    suspend fun getClientPlans(clientId: Long, status: String? = null): List<NutritionPlan>
    suspend fun getPlan(id: Long): NutritionPlan
    suspend fun updatePlan(id: Long, input: NutritionPlanInput): NutritionPlan
    suspend fun deactivatePlan(id: Long)
    suspend fun completePlan(id: Long)
    suspend fun getMyPlans(): List<NutritionPlan>
    suspend fun getMyActivePlans(): List<NutritionPlan>
    suspend fun getMyPlan(id: Long): NutritionPlan
    suspend fun getFoods(): List<Food>
    suspend fun getFood(id: Long): Food
    suspend fun createFood(input: FoodInput): Food
    suspend fun updateFood(id: Long, input: FoodInput): Food
    suspend fun activateFood(id: Long)
    suspend fun deactivateFood(id: Long)
}
