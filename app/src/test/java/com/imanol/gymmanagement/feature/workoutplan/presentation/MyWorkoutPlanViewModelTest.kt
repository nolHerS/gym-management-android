package com.imanol.gymmanagement.feature.workoutplan.presentation

import com.imanol.gymmanagement.feature.workoutplan.domain.CreateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.GetMyWorkoutWeekUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.UpdateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDayRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExerciseRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanRepository
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
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

    @Test
    fun identifiesCurrentAndNonCurrentWeeks() {
        assertTrue(isCurrentWeek("2026-09-14", "2026-09-14"))
        assertTrue(!isCurrentWeek("2026-09-07", "2026-09-14"))
    }

    @Test
    fun calculatesSundayForMondayWeekStart() {
        assertEquals("2026-09-20", weekEnd("2026-09-14"))
    }

    @Test
    fun newerWeekCannotBeOverwrittenByOlderResponse() {
        val repository = TestRepository()
        val weekA = CompletableDeferred<List<WorkoutPlan>>()
        val weekB = CompletableDeferred<List<WorkoutPlan>>()
        repository.loader = { week ->
            if (week == "2026-09-07") weekA.await() else weekB.await()
        }
        val viewModel = viewModel(repository)

        viewModel.loadWeek("2026-09-07")
        viewModel.loadWeek("2026-09-14")
        weekB.complete(listOf(plan("2026-09-14")))
        weekA.complete(listOf(plan("2026-09-07")))

        val state = viewModel.uiState.value as MyWorkoutPlanUiState.Success
        assertEquals("2026-09-14", state.weekStart)
        assertEquals("2026-09-14", state.plans.single().startDate)
    }

    @Test
    fun nonCooperativeOlderResponseCannotOverwriteNewerWeek() {
        val repository = TestRepository()
        val oldResponse = CompletableDeferred<List<WorkoutPlan>>()
        val newResponse = CompletableDeferred<List<WorkoutPlan>>()
        repository.loader = { week ->
            if (week == "2026-09-07") {
                try {
                    oldResponse.await()
                } catch (exception: kotlinx.coroutines.CancellationException) {
                    withContext(NonCancellable) { oldResponse.await() }
                }
            } else {
                newResponse.await()
            }
        }
        val viewModel = viewModel(repository)

        viewModel.loadWeek("2026-09-07")
        viewModel.loadWeek("2026-09-14")
        newResponse.complete(listOf(plan("2026-09-14")))
        oldResponse.complete(listOf(plan("2026-09-07")))

        val state = viewModel.uiState.value as MyWorkoutPlanUiState.Success
        assertEquals("2026-09-14", state.weekStart)
        assertEquals("2026-09-14", state.plans.single().startDate)
    }

    @Test
    fun cancellingPreviousLoadDoesNotBecomeAnError() {
        val repository = TestRepository()
        var cancellationObserved = false
        repository.loader = { week ->
            if (week == "2026-09-07") {
                try {
                    awaitCancellation()
                } catch (exception: kotlinx.coroutines.CancellationException) {
                    cancellationObserved = true
                    throw exception
                }
            }
            emptyList()
        }
        val viewModel = viewModel(repository)

        viewModel.loadWeek("2026-09-07")
        viewModel.loadWeek("2026-09-14")

        assertTrue(cancellationObserved)
        assertTrue(viewModel.uiState.value is MyWorkoutPlanUiState.Empty)
    }

    @Test
    fun duplicateLoadingOfSameWeekIsDeduplicated() {
        val repository = TestRepository()
        val gate = CompletableDeferred<List<WorkoutPlan>>()
        repository.loader = {
            repository.loadCalls++
            gate.await()
        }
        val viewModel = viewModel(repository)

        viewModel.loadWeek("2026-09-14")
        viewModel.loadWeek("2026-09-14")

        assertEquals(1, repository.loadCalls)
        gate.complete(emptyList())
    }

    @Test
    fun rapidNextWeekClicksKeepTheLatestSelectedWeek() {
        val repository = TestRepository()
        val latest = CompletableDeferred<List<WorkoutPlan>>()
        repository.loader = { week ->
            if (week == "2026-09-21") latest.await() else awaitCancellation()
        }
        val viewModel = viewModel(repository)

        viewModel.loadWeek("2026-09-07")
        viewModel.nextWeek()
        viewModel.nextWeek()
        latest.complete(emptyList())

        assertEquals("2026-09-21", viewModel.uiState.value.weekStart)
    }

    @Test
    fun retryUsesTheCurrentlySelectedWeek() {
        val repository = TestRepository()
        var attempts = 0
        repository.loader = {
            attempts++
            if (attempts == 1) throw IOException()
            emptyList()
        }
        val viewModel = viewModel(repository)

        viewModel.loadWeek("2026-09-14")
        viewModel.retry()

        assertEquals(listOf("2026-09-14", "2026-09-14"), repository.requestedWeeks)
        assertTrue(viewModel.uiState.value is MyWorkoutPlanUiState.Empty)
    }

    private fun viewModel(repository: TestRepository) = MyWorkoutPlanViewModel(
        GetMyWorkoutWeekUseCase(repository),
        CoroutineScope(Dispatchers.Unconfined),
    )
}

private class TestRepository : WorkoutPlanRepository {
    val requestedWeeks = mutableListOf<String>()
    var loadCalls = 0
    var result = emptyList<WorkoutPlan>()
    var failure: Exception? = null
    var loader: (suspend (String) -> List<WorkoutPlan>)? = null

    override suspend fun getMine() = result
    override suspend fun getMyWeek(weekStart: String): List<WorkoutPlan> {
        requestedWeeks += weekStart
        loader?.let { return it(weekStart) }
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

private fun plan(startDate: String) = WorkoutPlan(
    id = startDate.hashCode().toLong(),
    clientId = 2L,
    trainerId = 1L,
    sourceTemplateId = null,
    startDate = startDate,
    endDate = null,
    status = "ACTIVE",
    days = emptyList(),
)
