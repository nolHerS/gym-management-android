package com.imanol.gymmanagement.feature.nutrition.data.remote

import com.imanol.gymmanagement.feature.nutrition.domain.Food
import com.imanol.gymmanagement.feature.nutrition.domain.FoodInput
import com.imanol.gymmanagement.feature.nutrition.domain.MealFoodInput
import com.imanol.gymmanagement.feature.nutrition.domain.MealInput
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionPlan
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionPlanInput
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionPlanMeal
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionPlanMealFood

fun FoodResponse.toDomain() = Food(
    id, name, description, calories, protein, carbohydrates, fats, servingSize, servingUnit,
    active, createdAt, updatedAt,
)

fun NutritionPlanMealFoodResponse.toDomain() = NutritionPlanMealFood(
    id, food.toDomain(), quantity, unit, orderIndex,
)

fun NutritionPlanMealResponse.toDomain() = NutritionPlanMeal(
    id, name, description, orderIndex, foods.map { it.toDomain() },
)

fun NutritionPlanResponse.toDomain() = NutritionPlan(
    id, clientId, trainerId, name, description, startDate, endDate, status,
    meals.map { it.toDomain() }, createdAt, updatedAt,
)

fun MealFoodInput.toRemote() =
    NutritionPlanMealFoodRequest(foodId, quantity, unit, orderIndex)

fun MealInput.toRemote() =
    NutritionPlanMealRequest(name, description, orderIndex, foods.map { it.toRemote() })

fun NutritionPlanInput.toCreateRemote() =
    CreateNutritionPlanRequest(name, description, startDate, endDate, meals.map { it.toRemote() })

fun NutritionPlanInput.toUpdateRemote() = UpdateNutritionPlanRequest(
    name, description, startDate, endDate,
    requireNotNull(status) { "An update requires a status" },
    meals.map { it.toRemote() },
)

fun FoodInput.toRemote() = FoodRequest(
    name, description, calories, protein, carbohydrates, fats, servingSize, servingUnit,
)
