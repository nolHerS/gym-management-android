package com.imanol.gymmanagement.feature.nutrition.domain

import java.math.BigDecimal
import java.time.LocalDate

object NutritionPlanStatus {
    const val ACTIVE = "ACTIVE"
    const val INACTIVE = "INACTIVE"
    const val COMPLETED = "COMPLETED"
    val values = setOf(ACTIVE, INACTIVE, COMPLETED)
}

data class Food(
    val id: Long,
    val name: String,
    val description: String?,
    val calories: String?,
    val protein: String?,
    val carbohydrates: String?,
    val fats: String?,
    val servingSize: String?,
    val servingUnit: String?,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

data class NutritionPlanMealFood(
    val id: Long,
    val food: Food,
    val quantity: String,
    val unit: String,
    val orderIndex: Int,
)

data class NutritionPlanMeal(
    val id: Long,
    val name: String,
    val description: String?,
    val orderIndex: Int,
    val foods: List<NutritionPlanMealFood>,
)

data class NutritionPlan(
    val id: Long,
    val clientId: Long,
    val trainerId: Long,
    val name: String,
    val description: String?,
    val startDate: String,
    val endDate: String?,
    val status: String,
    val meals: List<NutritionPlanMeal>,
    val createdAt: String,
    val updatedAt: String,
)

data class MealFoodInput(
    val foodId: Long,
    val quantity: String,
    val unit: String,
    val orderIndex: Int,
)

data class MealInput(
    val name: String,
    val description: String?,
    val orderIndex: Int,
    val foods: List<MealFoodInput>,
)

data class NutritionPlanInput(
    val name: String,
    val description: String?,
    val startDate: String,
    val endDate: String?,
    val status: String? = null,
    val meals: List<MealInput>,
) {
    fun validationError(requireStatus: Boolean = status != null): String? {
        val start = runCatching { LocalDate.parse(startDate) }.getOrNull()
            ?: return "La fecha de inicio debe tener formato AAAA-MM-DD."
        val end = endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return when {
            name.isBlank() || name.length > 150 ->
                "El nombre es obligatorio y admite hasta 150 caracteres."
            endDate != null && end == null -> "La fecha de fin debe tener formato AAAA-MM-DD."
            end != null && end.isBefore(start) -> "La fecha de fin no puede ser anterior al inicio."
            requireStatus && status !in NutritionPlanStatus.values -> "El estado del plan no es válido."
            meals.isEmpty() -> "Añade al menos una comida."
            meals.any {
                it.name.isBlank() || it.name.length > 100 ||
                    it.orderIndex < 1 || it.foods.isEmpty()
            } ->
                "Cada comida necesita nombre, orden y al menos un alimento."
            meals.flatMap { it.foods }.any {
                it.foodId <= 0 || it.orderIndex < 1 ||
                    it.unit.isBlank() || it.unit.length > 30 ||
                    it.quantity.toPositiveDecimalOrNull() == null
            } -> "Revisa alimento, cantidad, unidad y orden."
            else -> null
        }
    }
}

data class FoodInput(
    val name: String,
    val description: String?,
    val calories: String?,
    val protein: String?,
    val carbohydrates: String?,
    val fats: String?,
    val servingSize: String?,
    val servingUnit: String?,
) {
    fun validationError(): String? = when {
        name.isBlank() || name.length > 150 -> "El nombre es obligatorio y admite hasta 150 caracteres."
        servingUnit != null && servingUnit.length > 30 -> "La unidad de ración admite hasta 30 caracteres."
        listOf(calories, protein, carbohydrates, fats, servingSize)
            .filterNotNull()
            .any { it.toNonNegativeDecimalOrNull() == null } ->
            "Los valores nutricionales y la ración deben ser números no negativos."
        else -> null
    }
}

private fun String.toPositiveDecimalOrNull(): BigDecimal? =
    runCatching { BigDecimal(this) }.getOrNull()?.takeIf { it > BigDecimal.ZERO }

private fun String.toNonNegativeDecimalOrNull(): BigDecimal? =
    runCatching { BigDecimal(this) }.getOrNull()?.takeIf { it >= BigDecimal.ZERO }
