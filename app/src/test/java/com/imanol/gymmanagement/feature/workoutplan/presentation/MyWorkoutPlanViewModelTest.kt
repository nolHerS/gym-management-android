package com.imanol.gymmanagement.feature.workoutplan.presentation

import com.imanol.gymmanagement.feature.workoutplan.domain.CreateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.GetMyWorkoutWeekUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.UpdateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDayRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExerciseRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanRepository
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class MyWorkoutPlanViewModelTest {
    @Test
    fun calculatesMondayAndMovesBetweenWeeks() {
        val repository = TestRepository()
        val viewModel = viewModel(repository)

        viewModel.loadWeek("2026-09-09")
        assertEquals("2026-09-07", repository.requestedWeeks.last())

        viewModel.previousWeek()
        assertEquals("2026-08-31", repository.requestedWeeks.last())

        viewModel.nextWeek()
        assertEquals("2026-09-07", repository.requestedWeeks.last())
    }

    @Test
    fun exposesEmptyErrorAndUnauthorizedStates() {
        val repository = TestRepository()
        val viewModel = viewModel(repository)

        repository.result = emptyList()
        viewModel.loadWeek("2026-09-07")
        assertTrue(viewModel.uiState.value is MyWorkoutPlanUiState.Empty)

        repository.failure = IOException()
        viewModel.loadWeek("2026-09-07")
        assertTrue(viewModel.uiState.value is MyWorkoutPlanUiState.Error)

        repository.failure = HttpException(Response.error<Unit>(401, "Unauthorized".toResponseBody()))
        viewModel.loadWeek("2026-09-07")
        assertTrue(viewModel.uiState.value is MyWorkoutPlanUiState.Unauthorized)
    }

    @Test
    fun mondayHelperKeepsMondayAndMovesSundayBack() {
        assertEquals("2026-09-07", "2026-09-07".mondayOfWeek())
        assertEquals("2026-09-07", "2026-09-13".mondayOfWeek())
    }

    private fun viewModel(repository: TestRepository) = MyWorkoutPlanViewModel(
        GetMyWorkoutWeekUseCase(repository),
        CoroutineScope(Dispatchers.Unconfined),
    )
}

private class TestRepository : WorkoutPlanRepository {
    val requestedWeeks = mutableListOf<String>()
    var result = emptyList<WorkoutPlan>()
    var failure: Exception? = null

    override suspend fun getMine() = result
    override suspend fun getMyWeek(weekStart: String): List<WorkoutPlan> {
        requestedWeeks += weekStart
        failure?.let { throw it }
        return result
    }

    override suspend fun create(clientId: Long, request: CreateWorkoutPlanRequest) =
        error("unused")
    override suspend fun getForClient(clientId: Long) = emptyList<WorkoutPlan>()
    override suspend fun get(id: Long) = error("unused")
    override suspend fun update(id: Long, request: UpdateWorkoutPlanRequest) = error("unused")
    override suspend fun addDay(planId: Long, request: WorkoutPlanDayRequest) = error("unused")
    override suspend fun deleteDay(planId: Long, day: Int) = Unit
    override suspend fun addExercise(planId: Long, day: Int, request: WorkoutPlanExerciseRequest) = error("unused")
    override suspend fun updateExercise(id: Long, request: WorkoutPlanExerciseRequest) = error("unused")
    override suspend fun deleteExercise(id: Long) = Unit
    override suspend fun deactivate(id: Long) = Unit
    override suspend fun complete(id: Long) = Unit
}
