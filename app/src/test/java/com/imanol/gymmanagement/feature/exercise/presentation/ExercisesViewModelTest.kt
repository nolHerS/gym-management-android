package com.imanol.gymmanagement.feature.exercise.presentation

import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseCategory
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseRepository
import com.imanol.gymmanagement.feature.exercise.domain.GetExercisesByCategoryUseCase
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ExercisesViewModelTest {
    private val exercise = Exercise(1L, "Bench Press", null, 2L, "Chest", true)

    @Test
    fun loadingBecomesSuccess() {
        val viewModel = viewModelFor { listOf(exercise) }

        viewModel.loadExercises(2L)

        assertEquals(ExercisesUiState.Success(listOf(exercise)), viewModel.uiState.value)
    }

    @Test
    fun emptyResponseProducesEmptyState() {
        val viewModel = viewModelFor { emptyList() }

        viewModel.loadExercises(2L)

        assertEquals(ExercisesUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun networkFailureProducesErrorState() {
        val viewModel = viewModelFor { throw IOException() }

        viewModel.loadExercises(2L)

        assertTrue(viewModel.uiState.value is ExercisesUiState.Error)
    }

    @Test
    fun unauthorizedResponseProducesUnauthorizedState() {
        val viewModel = viewModelFor {
            throw HttpException(Response.error<Unit>(401, "Unauthorized".toResponseBody()))
        }

        viewModel.loadExercises(2L)

        assertEquals(ExercisesUiState.Unauthorized, viewModel.uiState.value)
    }

    private fun viewModelFor(
        result: suspend () -> List<Exercise>,
    ): ExercisesViewModel =
        ExercisesViewModel(
            GetExercisesByCategoryUseCase(
                object : ExerciseRepository {
                    override suspend fun getExerciseCategories(): List<ExerciseCategory> =
                        emptyList()
                    override suspend fun getExercisesByCategory(categoryId: Long): List<Exercise> =
                        result()
                    override suspend fun getExerciseById(exerciseId: Long): Exercise =
                        error("Not used")
                },
            ),
            CoroutineScope(Dispatchers.Unconfined),
        )
}
