package com.imanol.gymmanagement.feature.nutrition

import com.imanol.gymmanagement.feature.nutrition.domain.Food
import com.imanol.gymmanagement.feature.nutrition.domain.FoodInput
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionPlan
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionPlanInput
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionRepository

class FakeNutritionRepository : NutritionRepository {
    var plan = samplePlan()
    var food = sampleFood()
    var plans = listOf(plan)
    var myPlans = listOf(plan)
    var activePlans = listOf(plan)
    var overrideFoods: List<Food>? = null
    var lastClientId: Long? = null
    var lastPlanId: Long? = null
    var lastStatus: String? = null
    var lastInput: NutritionPlanInput? = null
    var lastFoodInput: FoodInput? = null
    var deactivatedPlanId: Long? = null
    var completedPlanId: Long? = null
    var activatedFoodId: Long? = null
    var deactivatedFoodId: Long? = null
    var failure: Throwable? = null

    override suspend fun createPlan(clientId: Long, input: NutritionPlanInput): NutritionPlan {
        failure?.let { throw it }
        lastClientId = clientId
        lastInput = input
        return plan
    }

    override suspend fun getClientPlans(clientId: Long, status: String?): List<NutritionPlan> {
        failure?.let { throw it }
        lastClientId = clientId
        lastStatus = status
        return plans
    }

    override suspend fun getPlan(id: Long): NutritionPlan {
        failure?.let { throw it }
        lastPlanId = id
        return plan
    }

    override suspend fun updatePlan(id: Long, input: NutritionPlanInput): NutritionPlan {
        failure?.let { throw it }
        lastPlanId = id
        lastInput = input
        return plan
    }

    override suspend fun deactivatePlan(id: Long) {
        failure?.let { throw it }
        deactivatedPlanId = id
    }

    override suspend fun completePlan(id: Long) {
        failure?.let { throw it }
        completedPlanId = id
    }

    override suspend fun getMyPlans(): List<NutritionPlan> {
        failure?.let { throw it }
        return myPlans
    }

    override suspend fun getMyActivePlans(): List<NutritionPlan> {
        failure?.let { throw it }
        return activePlans
    }

    override suspend fun getMyPlan(id: Long): NutritionPlan {
        failure?.let { throw it }
        lastPlanId = id
        return plan
    }

    override suspend fun getFoods(): List<Food> {
        failure?.let { throw it }
        return overrideFoods ?: listOf(food)
    }

    override suspend fun getFood(id: Long): Food {
        failure?.let { throw it }
        return food
    }

    override suspend fun createFood(input: FoodInput): Food {
        failure?.let { throw it }
        lastFoodInput = input
        return food
    }

    override suspend fun updateFood(id: Long, input: FoodInput): Food {
        failure?.let { throw it }
        lastPlanId = id
        lastFoodInput = input
        return food
    }

    override suspend fun activateFood(id: Long) {
        failure?.let { throw it }
        activatedFoodId = id
    }

    override suspend fun deactivateFood(id: Long) {
        failure?.let { throw it }
        deactivatedFoodId = id
    }
}

fun sampleFood() = Food(
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

fun samplePlan() = NutritionPlan(
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
