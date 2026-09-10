package com.imanol.gymmanagement.feature.nutrition.presentation

import com.imanol.gymmanagement.feature.nutrition.FakeNutritionRepository
import com.imanol.gymmanagement.feature.nutrition.domain.GetFoodUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.GetFoodsUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.SaveFoodUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.SetFoodActiveUseCase
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class FoodViewModelTest {
    @Test
    fun loadReportsSuccessAndEmpty() {
        val repository = FakeNutritionRepository()
        val viewModel = viewModel(repository)

        viewModel.loadFoods()
        assertTrue(viewModel.foods.value is FoodsUiState.Success)

        viewModel.loadFoods()
        repository.failure = null
        assertTrue((viewModel.foods.value as FoodsUiState.Success).foods.isNotEmpty())
    }

    @Test
    fun loadReportsEmptyAndNetworkError() {
        val repository = FakeNutritionRepository()
        val viewModel = viewModel(repository)

        repository.overrideFoods = emptyList()
        viewModel.loadFoods()
        assertEquals(FoodsUiState.Empty, viewModel.foods.value)

        repository.failure = IOException()
        viewModel.loadFoods()
        assertEquals(
            NutritionFailure.NETWORK,
            (viewModel.foods.value as FoodsUiState.Failure).problem.failure,
        )
    }

    @Test
    fun createEditAndToggleFoodDelegateOperations() {
        val repository = FakeNutritionRepository()
        val viewModel = viewModel(repository)

        viewModel.prepareForm(null)
        viewModel.updateForm { it.copy(name = "Avena", calories = "389") }
        viewModel.save(null)
        assertEquals(1L, viewModel.form.value.savedFoodId)

        viewModel.prepareForm(1L)
        viewModel.save(1L)
        assertEquals(1L, repository.lastPlanId)

        viewModel.loadFood(1L)
        viewModel.setActive(repository.food)
        assertEquals(1L, repository.deactivatedFoodId)
    }

    @Test
    fun detailMapsUnauthorizedError() {
        val repository = FakeNutritionRepository()
        repository.failure = HttpException(
            Response.error<Unit>(403, "Forbidden".toResponseBody()),
        )
        val viewModel = viewModel(repository)

        viewModel.loadFood(1L)

        assertEquals(
            NutritionFailure.FORBIDDEN,
            (viewModel.detail.value as FoodDetailUiState.Failure).problem.failure,
        )
    }

    private fun viewModel(repository: FakeNutritionRepository) = FoodViewModel(
        GetFoodsUseCase(repository),
        GetFoodUseCase(repository),
        SaveFoodUseCase(repository),
        SetFoodActiveUseCase(repository),
        CoroutineScope(Dispatchers.Unconfined),
    )
}
