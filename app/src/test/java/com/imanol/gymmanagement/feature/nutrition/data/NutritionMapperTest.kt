package com.imanol.gymmanagement.feature.nutrition.data

import com.imanol.gymmanagement.feature.nutrition.data.remote.FoodResponse
import com.imanol.gymmanagement.feature.nutrition.data.remote.NutritionPlanMealFoodResponse
import com.imanol.gymmanagement.feature.nutrition.data.remote.NutritionPlanMealResponse
import com.imanol.gymmanagement.feature.nutrition.data.remote.NutritionPlanResponse
import com.imanol.gymmanagement.feature.nutrition.data.remote.toDomain
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionMapperTest {
    @Test
    fun mapsNestedNutritionPlanAndFood() {
        val food = FoodResponse(
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
        val plan = NutritionPlanResponse(
            id = 10L,
            clientId = 2L,
            trainerId = 1L,
            name = "Definición",
            description = "Inicial",
            startDate = "2026-09-07",
            endDate = null,
            status = "ACTIVE",
            meals = listOf(
                NutritionPlanMealResponse(
                    id = 20L,
                    name = "Desayuno",
                    description = "Primera comida",
                    orderIndex = 1,
                    foods = listOf(
                        NutritionPlanMealFoodResponse(30L, food, "100", "g", 1),
                    ),
                ),
            ),
            createdAt = "2026-09-09T16:55:00",
            updatedAt = "2026-09-09T16:55:00",
        )

        val domain = plan.toDomain()

        assertEquals("Definición", domain.name)
        assertEquals("Avena", domain.meals.single().foods.single().food.name)
        assertEquals("16.9", domain.meals.single().foods.single().food.protein)
    }
}
