package com.imanol.gymmanagement.feature.nutrition.presentation

import com.imanol.gymmanagement.feature.nutrition.FakeNutritionRepository
import com.imanol.gymmanagement.feature.nutrition.domain.GetMyActiveNutritionPlanUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.GetMyNutritionPlanUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.GetMyNutritionPlansUseCase
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class MyNutritionViewModelTest {
    @Test
    fun loadUsesMineAndActiveEndpointsAndReportsSuccess() {
        val repository = FakeNutritionRepository()
        val viewModel = viewModel(repository)

        viewModel.load()

        assertTrue(viewModel.plans.value is MyNutritionUiState.Success)
        assertTrue((viewModel.plans.value as MyNutritionUiState.Success).plans.isNotEmpty())
        assertTrue((viewModel.plans.value as MyNutritionUiState.Success).activePlans.isNotEmpty())
    }

    @Test
    fun loadReportsEmptyAndNetworkError() {
        val repository = FakeNutritionRepository()
        val viewModel = viewModel(repository)

        repository.myPlans = emptyList()
        repository.activePlans = emptyList()
        viewModel.load()
        assertEquals(MyNutritionUiState.Empty, viewModel.plans.value)

        repository.failure = IOException()
        viewModel.load()
        assertEquals(
            NutritionFailure.NETWORK,
            (viewModel.plans.value as MyNutritionUiState.Failure).problem.failure,
        )
    }

    @Test
    fun loadMapsUnauthorizedAndDetailUsesPlanId() {
        val repository = FakeNutritionRepository()
        repository.failure = HttpException(
            Response.error<Unit>(401, "Unauthorized".toResponseBody()),
        )
        val viewModel = viewModel(repository)

        viewModel.load()
        assertEquals(
            NutritionFailure.UNAUTHORIZED,
            (viewModel.plans.value as MyNutritionUiState.Failure).problem.failure,
        )

        repository.failure = null
        val detailViewModel = viewModel(repository)
        detailViewModel.loadDetail(10L)
        assertTrue("lastPlanId=${repository.lastPlanId}", repository.lastPlanId == 10L)
    }

    private fun viewModel(repository: FakeNutritionRepository) = MyNutritionViewModel(
        GetMyNutritionPlansUseCase(repository),
        GetMyActiveNutritionPlanUseCase(repository),
        GetMyNutritionPlanUseCase(repository),
        CoroutineScope(Dispatchers.Unconfined),
    )
}
