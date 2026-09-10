package com.imanol.gymmanagement.feature.nutrition.domain

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionValidationTest {
    private val validMeal = MealInput(
        name = "Desayuno",
        description = null,
        orderIndex = 1,
        foods = listOf(MealFoodInput(1L, "100.00", "g", 1)),
    )

    @Test
    fun acceptsPrecisePositiveQuantityAndSameDates() {
        assertNull(
            NutritionPlanInput(
                name = "Plan",
                description = null,
                startDate = "2026-09-07",
                endDate = "2026-09-07",
                meals = listOf(validMeal),
            ).validationError(),
        )
    }

    @Test
    fun rejectsNegativeNutritionValue() {
        assertNotNull(
            FoodInput(
                name = "Avena",
                description = null,
                calories = "-1",
                protein = null,
                carbohydrates = null,
                fats = null,
                servingSize = null,
                servingUnit = "g",
            ).validationError(),
        )
    }

    @Test
    fun rejectsEmptyMealFoods() {
        assertNotNull(
            NutritionPlanInput(
                name = "Plan",
                description = null,
                startDate = "2026-09-07",
                endDate = null,
                meals = listOf(validMeal.copy(foods = emptyList())),
            ).validationError(),
        )
    }
}
