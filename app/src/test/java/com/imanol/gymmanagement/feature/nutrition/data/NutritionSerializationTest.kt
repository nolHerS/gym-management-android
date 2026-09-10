package com.imanol.gymmanagement.feature.nutrition.data

import com.imanol.gymmanagement.feature.nutrition.data.remote.CreateNutritionPlanRequest
import com.imanol.gymmanagement.feature.nutrition.data.remote.DecimalStringSerializer
import com.imanol.gymmanagement.feature.nutrition.data.remote.FoodRequest
import com.imanol.gymmanagement.feature.nutrition.data.remote.NutritionPlanMealFoodRequest
import com.imanol.gymmanagement.feature.nutrition.data.remote.NutritionPlanMealRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionSerializationTest {
    private val json = Json {
        explicitNulls = true
        encodeDefaults = true
    }

    @Test
    fun quantityIsSerializedAsNumberWithoutFloatingPointConversion() {
        val request = NutritionPlanMealFoodRequest(1L, "100.12345678901234567890", "g", 1)

        val encoded = json.encodeToString(request)

        assertTrue("encoded=$encoded", encoded.contains("\"quantity\":100.12345678901234567890"))
        assertFalse(encoded.contains("\"quantity\":\""))
    }

    @Test
    fun nutritionalValuesAreSerializedAsNumbersAndNullsRemainNull() {
        val request = FoodRequest(
            name = "Avena",
            description = null,
            calories = "389.000000000000000001",
            protein = "16.900000000000000002",
            carbohydrates = "66.300000000000000003",
            fats = "6.900000000000000004",
            servingSize = "100.000000000000000005",
            servingUnit = null,
        )

        val encoded = json.encodeToString(request)

        assertTrue("encoded=$encoded", encoded.contains("\"calories\":389.000000000000000001"))
        assertTrue(encoded.contains("\"protein\":16.900000000000000002"))
        assertTrue(encoded.contains("\"carbohydrates\":66.300000000000000003"))
        assertTrue(encoded.contains("\"fats\":6.900000000000000004"))
        assertTrue(encoded.contains("\"servingSize\":100.000000000000000005"))
        assertTrue(encoded.contains("\"description\":null"))
        assertTrue(encoded.contains("\"servingUnit\":null"))
    }
}
