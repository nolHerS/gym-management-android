package com.imanol.gymmanagement.feature.exercise.presentation

import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseCategory
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseRepository
import com.imanol.gymmanagement.feature.exercise.domain.GetExerciseCategoriesUseCase
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ExerciseCategoriesViewModelTest {
    private val categories = listOf(
        ExerciseCategory(
            id = 1L,
            name = "Chest",
            active = true,
            createdAt = null,
            updatedAt = null,
        ),
    )

    @Test
    fun loadingBecomesSuccess() {
        val viewModel = viewModelReturning(categories)

        viewModel.loadCategories()

        assertEquals(ExerciseCategoriesUiState.Success(categories), viewModel.uiState.value)
    }

    @Test
    fun loadingBecomesErrorForNetworkFailure() {
        val viewModel = viewModelThrowing(IOException())

        viewModel.loadCategories()

        assertTrue(viewModel.uiState.value is ExerciseCategoriesUiState.Error)
    }

    @Test
    fun unauthorizedResponseProducesUnauthorizedState() {
        val viewModel = viewModelThrowing(
            HttpException(Response.error<Unit>(401, "Unauthorized".toResponseBody())),
        )

        viewModel.loadCategories()

        assertEquals(ExerciseCategoriesUiState.Unauthorized, viewModel.uiState.value)
    }

    @Test
    fun retryAfterErrorLoadsCategories() {
        var calls = 0
        val useCase = GetExerciseCategoriesUseCase(
            object : ExerciseRepository {
                override suspend fun getExerciseCategories(): List<ExerciseCategory> {
                    calls++
                    if (calls == 1) throw IOException()
                    return categories
                }
                override suspend fun getExercisesByCategory(categoryId: Long): List<Exercise> =
                    emptyList()
                override suspend fun getExerciseById(exerciseId: Long): Exercise =
                    error("Not used")
            },
        )
        val viewModel = ExerciseCategoriesViewModel(
            useCase,
            CoroutineScope(Dispatchers.Unconfined),
        )

        viewModel.loadCategories()
        viewModel.loadCategories()

        assertEquals(ExerciseCategoriesUiState.Success(categories), viewModel.uiState.value)
        assertEquals(2, calls)
    }

    private fun viewModelReturning(
        categories: List<ExerciseCategory>,
    ): ExerciseCategoriesViewModel =
        ExerciseCategoriesViewModel(
            GetExerciseCategoriesUseCase(
                object : ExerciseRepository {
                    override suspend fun getExerciseCategories(): List<ExerciseCategory> =
                        categories
                    override suspend fun getExercisesByCategory(categoryId: Long): List<Exercise> =
                        emptyList()
                    override suspend fun getExerciseById(exerciseId: Long): Exercise =
                        error("Not used")
                },
            ),
            CoroutineScope(Dispatchers.Unconfined),
        )

    private fun viewModelThrowing(exception: Exception): ExerciseCategoriesViewModel =
        ExerciseCategoriesViewModel(
            GetExerciseCategoriesUseCase(
                object : ExerciseRepository {
                    override suspend fun getExerciseCategories(): List<ExerciseCategory> =
                        throw exception
                    override suspend fun getExercisesByCategory(categoryId: Long): List<Exercise> =
                        emptyList()
                    override suspend fun getExerciseById(exerciseId: Long): Exercise =
                        error("Not used")
                },
            ),
            CoroutineScope(Dispatchers.Unconfined),
        )
}
