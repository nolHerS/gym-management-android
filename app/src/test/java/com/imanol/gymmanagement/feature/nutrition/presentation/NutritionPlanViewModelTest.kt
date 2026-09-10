package com.imanol.gymmanagement.feature.nutrition.presentation

import com.imanol.gymmanagement.feature.nutrition.FakeNutritionRepository
import com.imanol.gymmanagement.feature.nutrition.domain.CompleteNutritionPlanUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.CreateNutritionPlanUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.DeactivateNutritionPlanUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.GetClientNutritionPlansUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.GetFoodsUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.GetNutritionPlanUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.UpdateNutritionPlanUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class NutritionPlanViewModelTest {
    @Test
    fun listReportsSuccessAndEmpty() {
        val repository = FakeNutritionRepository()
        val viewModel = viewModel(repository)

        viewModel.loadPlans(2L, "ACTIVE")
        assertTrue(viewModel.plans.value is NutritionPlansUiState.Success)
        assertEquals(2L, repository.lastClientId)
        assertEquals("ACTIVE", repository.lastStatus)

        repository.plans = emptyList()
        viewModel.loadPlans(2L, null)
        assertEquals(NutritionPlansUiState.Empty, viewModel.plans.value)
    }

    @Test
    fun listMaps404And409ToSpecificFailures() {
        val repository = FakeNutritionRepository()
        val viewModel = viewModel(repository)

        repository.failure = httpException(404)
        viewModel.loadPlans(2L)
        assertEquals(
            NutritionFailure.NOT_FOUND,
            (viewModel.plans.value as NutritionPlansUiState.Failure).problem.failure,
        )

        repository.failure = httpException(409)
        viewModel.loadPlans(2L)
        assertEquals(
            NutritionFailure.CONFLICT,
            (viewModel.plans.value as NutritionPlansUiState.Failure).problem.failure,
        )
    }

    @Test
    fun createAndUpdateSubmitValidatedInputs() {
        val repository = FakeNutritionRepository()
        val viewModel = viewModel(repository)
        viewModel.updatePlanInfo(
            name = "Plan nuevo",
            startDate = "2026-09-07",
            endDate = "",
        )
        viewModel.addMeal()
        viewModel.updateMeal(1L) { it.copy(name = "Desayuno") }
        viewModel.addFood(1L, 1L)

        viewModel.save(2L, null)
        assertTrue("state=${viewModel.form.value}", viewModel.form.value.savedPlanId == 10L)
        assertEquals(2L, repository.lastClientId)

        viewModel.prepareForm(10L)
        viewModel.addMeal()
        viewModel.updateMeal(3L) { it.copy(name = "Comida") }
        viewModel.addFood(3L, 1L)
        viewModel.save(2L, 10L)
        assertEquals(10L, repository.lastPlanId)
    }

    @Test
    fun completeAndDeactivateDelegateActions() {
        val repository = FakeNutritionRepository()
        val viewModel = viewModel(repository)

        viewModel.loadDetail(10L)
        viewModel.complete(10L)
        assertEquals(10L, repository.completedPlanId)

        viewModel.loadDetail(10L)
        viewModel.deactivate(10L)
        assertEquals(10L, repository.deactivatedPlanId)
    }

    @Test
    fun detailMapsErrors() {
        val repository = FakeNutritionRepository()
        val viewModel = viewModel(repository)

        repository.failure = httpException(400)
        viewModel.loadDetail(10L)
        assertEquals(
            NutritionFailure.VALIDATION,
            (viewModel.detail.value as NutritionPlanDetailUiState.Failure).problem.failure,
        )

        repository.failure = httpException(401)
        viewModel.loadDetail(10L)
        assertEquals(
            NutritionFailure.UNAUTHORIZED,
            (viewModel.detail.value as NutritionPlanDetailUiState.Failure).problem.failure,
        )
    }

    private fun viewModel(repository: FakeNutritionRepository) = NutritionPlanViewModel(
        GetClientNutritionPlansUseCase(repository),
        GetNutritionPlanUseCase(repository),
        CreateNutritionPlanUseCase(repository),
        UpdateNutritionPlanUseCase(repository),
        DeactivateNutritionPlanUseCase(repository),
        CompleteNutritionPlanUseCase(repository),
        GetFoodsUseCase(repository),
        CoroutineScope(Dispatchers.Unconfined),
    )

    private fun httpException(code: Int) =
        HttpException(Response.error<Unit>(code, code.toString().toResponseBody()))
}
