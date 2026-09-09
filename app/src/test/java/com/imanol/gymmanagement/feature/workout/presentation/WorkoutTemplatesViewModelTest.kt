package com.imanol.gymmanagement.feature.workout.presentation

import com.imanol.gymmanagement.feature.workout.domain.ActivateWorkoutTemplateUseCase
import com.imanol.gymmanagement.feature.workout.domain.CreateWorkoutTemplateUseCase
import com.imanol.gymmanagement.feature.workout.domain.DeactivateWorkoutTemplateUseCase
import com.imanol.gymmanagement.feature.workout.domain.GetWorkoutTemplateDetailUseCase
import com.imanol.gymmanagement.feature.workout.domain.GetWorkoutTemplatesUseCase
import com.imanol.gymmanagement.feature.workout.domain.UpdateWorkoutTemplateUseCase
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplate
import com.imanol.gymmanagement.feature.workout.domain.WorkoutRepository
import com.imanol.gymmanagement.feature.workout.domain.EmptyWorkoutRepository
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class WorkoutTemplatesViewModelTest {
    private val template = WorkoutTemplate(1L, "Strength", "Three days", true)

    @Test
    fun loadingBecomesSuccess() {
        val viewModel = viewModelFor { listOf(template) }

        viewModel.loadTemplates()

        assertEquals(
            WorkoutTemplatesUiState.Success(listOf(template)),
            viewModel.uiState.value,
        )
    }

    @Test
    fun emptyResponseProducesEmptyState() {
        val viewModel = viewModelFor { emptyList() }

        viewModel.loadTemplates()

        assertEquals(WorkoutTemplatesUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun networkFailureProducesErrorState() {
        val viewModel = viewModelFor { throw IOException() }

        viewModel.loadTemplates()

        assertTrue(viewModel.uiState.value is WorkoutTemplatesUiState.Error)
    }

    @Test
    fun unauthorizedResponseProducesUnauthorizedState() {
        val viewModel = viewModelFor {
            throw HttpException(Response.error<Unit>(401, "Unauthorized".toResponseBody()))
        }

        viewModel.loadTemplates()

        assertEquals(WorkoutTemplatesUiState.Unauthorized, viewModel.uiState.value)
    }

    private fun viewModelFor(
        result: suspend () -> List<WorkoutTemplate>,
    ): WorkoutTemplatesViewModel {
        val repository = object : EmptyWorkoutRepository() {
            override suspend fun getWorkoutTemplates(): List<WorkoutTemplate> = result()
        }
        return WorkoutTemplatesViewModel(
            GetWorkoutTemplatesUseCase(repository),
            GetWorkoutTemplateDetailUseCase(repository),
            CreateWorkoutTemplateUseCase(repository),
            UpdateWorkoutTemplateUseCase(repository),
            ActivateWorkoutTemplateUseCase(repository),
            DeactivateWorkoutTemplateUseCase(repository),
            CoroutineScope(Dispatchers.Unconfined),
        )
    }
}
