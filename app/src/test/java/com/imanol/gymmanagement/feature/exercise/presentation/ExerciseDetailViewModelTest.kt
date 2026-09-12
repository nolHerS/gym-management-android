package com.imanol.gymmanagement.feature.exercise.presentation

import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseCategory
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseRepository
import com.imanol.gymmanagement.feature.exercise.domain.GetExerciseDetailUseCase
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ExerciseDetailViewModelTest {
    private val exercise = Exercise(1L, "Bench Press", null, 2L, "Chest", true)

    @Test
    fun loadingBecomesSuccess() {
        val viewModel = viewModelFor(null)

        viewModel.loadExercise(1L)

        assertEquals(ExerciseDetailUiState.Success(exercise), viewModel.uiState.value)
    }

    @Test
    fun networkFailureProducesErrorState() {
        val viewModel = viewModelFor(IOException())

        viewModel.loadExercise(1L)

        assertTrue(viewModel.uiState.value is ExerciseDetailUiState.Error)
    }

    @Test
    fun unauthorizedResponseProducesUnauthorizedState() {
        val viewModel = viewModelFor(
            HttpException(Response.error<Unit>(401, "Unauthorized".toResponseBody())),
        )

        viewModel.loadExercise(1L)

        assertEquals(ExerciseDetailUiState.Unauthorized, viewModel.uiState.value)
    }

    @Test
    fun notFoundResponseProducesErrorState() {
        val viewModel = viewModelFor(
            HttpException(Response.error<Unit>(404, "Not found".toResponseBody())),
        )

        viewModel.loadExercise(1L)

        assertTrue(viewModel.uiState.value is ExerciseDetailUiState.Error)
    }

    @Test
    fun serializationFailureProducesErrorState() {
        val viewModel = viewModelFor(SerializationException("invalid json"))

        viewModel.loadExercise(1L)

        assertTrue(viewModel.uiState.value is ExerciseDetailUiState.Error)
    }

    private fun viewModelFor(exception: Exception?): ExerciseDetailViewModel =
        ExerciseDetailViewModel(
            GetExerciseDetailUseCase(
                object : ExerciseRepository {
                    override suspend fun getExerciseCategories(): List<ExerciseCategory> =
                        emptyList()
                    override suspend fun getExercisesByCategory(categoryId: Long): List<Exercise> =
                        emptyList()
                    override suspend fun getExerciseById(exerciseId: Long): Exercise {
                        if (exception != null) throw exception
                        return exercise
                    }
                },
            ),
            CoroutineScope(Dispatchers.Unconfined),
        )
}
