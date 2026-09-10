package com.imanol.gymmanagement.feature.nutrition.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class NutritionPlanMealFoodRequest(
    val foodId: Long,
    @Serializable(with = DecimalStringSerializer::class)
    val quantity: String,
    val unit: String,
    val orderIndex: Int,
)

@Serializable
data class NutritionPlanMealRequest(
    val name: String,
    val description: String? = null,
    val orderIndex: Int,
    val foods: List<NutritionPlanMealFoodRequest>,
)

@Serializable
data class CreateNutritionPlanRequest(
    val name: String,
    val description: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val meals: List<NutritionPlanMealRequest>,
)

@Serializable
data class UpdateNutritionPlanRequest(
    val name: String,
    val description: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val status: String,
    val meals: List<NutritionPlanMealRequest>,
)

@Serializable
data class FoodRequest(
    val name: String,
    val description: String? = null,
    @Serializable(with = DecimalStringSerializer::class)
    val calories: String? = null,
    @Serializable(with = DecimalStringSerializer::class)
    val protein: String? = null,
    @Serializable(with = DecimalStringSerializer::class)
    val carbohydrates: String? = null,
    @Serializable(with = DecimalStringSerializer::class)
    val fats: String? = null,
    @Serializable(with = DecimalStringSerializer::class)
    val servingSize: String? = null,
    val servingUnit: String? = null,
)

@Serializable
data class FoodResponse(
    val id: Long,
    val name: String,
    val description: String? = null,
    @Serializable(with = DecimalStringSerializer::class)
    val calories: String? = null,
    @Serializable(with = DecimalStringSerializer::class)
    val protein: String? = null,
    @Serializable(with = DecimalStringSerializer::class)
    val carbohydrates: String? = null,
    @Serializable(with = DecimalStringSerializer::class)
    val fats: String? = null,
    @Serializable(with = DecimalStringSerializer::class)
    val servingSize: String? = null,
    val servingUnit: String? = null,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class NutritionPlanMealFoodResponse(
    val id: Long,
    val food: FoodResponse,
    @Serializable(with = DecimalStringSerializer::class)
    val quantity: String,
    val unit: String,
    val orderIndex: Int,
)

@Serializable
data class NutritionPlanMealResponse(
    val id: Long,
    val name: String,
    val description: String? = null,
    val orderIndex: Int,
    val foods: List<NutritionPlanMealFoodResponse> = emptyList(),
)

@Serializable
data class NutritionPlanResponse(
    val id: Long,
    val clientId: Long,
    val trainerId: Long,
    val name: String,
    val description: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val status: String,
    val meals: List<NutritionPlanMealResponse> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)
