package com.imanol.gymmanagement.feature.workout.presentation

import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseCategory
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseRepository
import com.imanol.gymmanagement.feature.exercise.domain.GetExerciseCategoriesUseCase
import com.imanol.gymmanagement.feature.exercise.domain.GetExercisesByCategoryUseCase
import com.imanol.gymmanagement.feature.workout.domain.AddExerciseToWorkoutTemplateUseCase
import com.imanol.gymmanagement.feature.workout.domain.DeleteWorkoutTemplateExerciseUseCase
import com.imanol.gymmanagement.feature.workout.domain.GetWorkoutTemplateDetailUseCase
import com.imanol.gymmanagement.feature.workout.domain.UpdateWorkoutTemplateExerciseUseCase
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplate
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplateDetail
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

class WorkoutTemplateDetailViewModelTest {
    private val detail = WorkoutTemplateDetail(
        WorkoutTemplate(1L, "Strength", null, true),
        emptyList(),
    )

    @Test
    fun loadingBecomesSuccess() {
        val viewModel = viewModelFor(null)

        viewModel.loadDetail(1L)

        assertEquals(
            WorkoutTemplateDetailUiState.Success(detail),
            viewModel.uiState.value,
        )
    }

    @Test
    fun networkFailureProducesErrorState() {
        val viewModel = viewModelFor(IOException())

        viewModel.loadDetail(1L)

        assertTrue(viewModel.uiState.value is WorkoutTemplateDetailUiState.Error)
    }

    @Test
    fun unauthorizedResponseProducesUnauthorizedState() {
        val viewModel = viewModelFor(
            HttpException(Response.error<Unit>(401, "Unauthorized".toResponseBody())),
        )

        viewModel.loadDetail(1L)

        assertEquals(
            WorkoutTemplateDetailUiState.Unauthorized,
            viewModel.uiState.value,
        )
    }

    private fun viewModelFor(exception: Exception?): WorkoutTemplateDetailViewModel {
        val workoutRepository = object : EmptyWorkoutRepository() {
            override suspend fun getWorkoutTemplateDetail(
                templateId: Long,
            ): WorkoutTemplateDetail {
                if (exception != null) throw exception
                return detail
            }
        }
        val exerciseRepository = object : ExerciseRepository {
            override suspend fun getExerciseCategories(): List<ExerciseCategory> =
                emptyList()
            override suspend fun getExercisesByCategory(categoryId: Long): List<Exercise> =
                emptyList()
            override suspend fun getExerciseById(exerciseId: Long): Exercise =
                error("Not used")
        }
        return WorkoutTemplateDetailViewModel(
            GetWorkoutTemplateDetailUseCase(workoutRepository),
            AddExerciseToWorkoutTemplateUseCase(workoutRepository),
            UpdateWorkoutTemplateExerciseUseCase(workoutRepository),
            DeleteWorkoutTemplateExerciseUseCase(workoutRepository),
            GetExerciseCategoriesUseCase(exerciseRepository),
            GetExercisesByCategoryUseCase(exerciseRepository),
            CoroutineScope(Dispatchers.Unconfined),
        )
    }
}
