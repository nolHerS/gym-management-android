package com.imanol.gymmanagement.feature.nutrition.domain

import com.imanol.gymmanagement.feature.nutrition.FakeNutritionRepository
import com.imanol.gymmanagement.feature.nutrition.sampleFood
import com.imanol.gymmanagement.feature.nutrition.samplePlan
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class NutritionUseCasesTest {
    private val validInput = NutritionPlanInput(
        name = "Plan",
        description = null,
        startDate = "2026-09-07",
        endDate = null,
        meals = listOf(
            MealInput(
                name = "Desayuno",
                description = null,
                orderIndex = 1,
                foods = listOf(MealFoodInput(1L, "100.25", "g", 1)),
            ),
        ),
    )

    @Test
    fun createRejectsInvalidInputBeforeRepository() = runBlocking {
        val repository = FakeNutritionRepository()
        val invalid = validInput.copy(name = "", meals = emptyList())

        try {
            CreateNutritionPlanUseCase(repository)(2L, invalid)
            fail("Expected validation error")
        } catch (_: IllegalArgumentException) {
        }

        assertEquals(null, repository.lastClientId)
    }

    @Test
    fun updateRequiresValidStatusAndDelegatesValidInput() = runBlocking {
        val repository = FakeNutritionRepository()

        try {
            UpdateNutritionPlanUseCase(repository)(10L, validInput)
            fail("Expected validation error")
        } catch (_: IllegalArgumentException) {
        }

        val updated = UpdateNutritionPlanUseCase(repository)(
            10L,
            validInput.copy(status = NutritionPlanStatus.COMPLETED),
        )

        assertEquals(samplePlan(), updated)
        assertEquals(10L, repository.lastPlanId)
    }

    @Test
    fun foodSaveAndActivationDelegateOperations() = runBlocking {
        val repository = FakeNutritionRepository()
        val input = FoodInput("Avena", null, "389", "16.9", "66.3", "6.9", "100", "g")

        assertEquals(sampleFood(), SaveFoodUseCase(repository)(null, input))
        assertEquals(sampleFood(), SaveFoodUseCase(repository)(1L, input))

        SetFoodActiveUseCase(repository)(1L, true)
        assertEquals(1L, repository.activatedFoodId)
        SetFoodActiveUseCase(repository)(1L, false)
        assertEquals(1L, repository.deactivatedFoodId)
    }
}
