package com.imanol.gymmanagement.feature.workoutexecution.presentation

import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.workoutexecution.domain.CreateWorkoutExecutionSnapshotUseCase
import com.imanol.gymmanagement.feature.workoutexecution.domain.SelectWorkoutPlanForExecutionUseCase
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionDateProvider
import com.imanol.gymmanagement.feature.workoutplan.domain.CreateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.GetMyWorkoutWeekUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.UpdateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDay
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDayRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExercise
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExerciseRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
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

class WorkoutTodayViewModelTest {
    private val dateProvider = FixedDateProvider("2026-09-09", 1234L)

    @Test
    fun loadingResolvesSuccessAndStartCreatesSnapshot() {
        val repository = TestRepository().also { it.result = listOf(plan()) }
        val viewModel = viewModel(repository)

        viewModel.loadToday()

        val state = viewModel.uiState.value as WorkoutTodayUiState.Success
        assertEquals(3, state.dayOfWeek)
        var snapshotPlanId: Long? = null
        viewModel.createSnapshot { snapshot -> snapshotPlanId = snapshot.planId }
        assertEquals(10L, snapshotPlanId)
    }

    @Test
    fun exposesEmptyAmbiguousErrorAndUnauthorizedStates() {
        val repository = TestRepository()
        val viewModel = viewModel(repository)

        repository.result = emptyList()
        viewModel.loadToday()
        assertTrue(viewModel.uiState.value is WorkoutTodayUiState.Empty)

        repository.result = listOf(plan(), plan(11L))
        viewModel.loadToday()
        assertTrue(viewModel.uiState.value is WorkoutTodayUiState.Ambiguous)

        repository.failure = IOException()
        viewModel.loadToday()
        assertTrue(viewModel.uiState.value is WorkoutTodayUiState.Error)

        repository.failure = HttpException(Response.error<Unit>(401, "Unauthorized".toResponseBody()))
        viewModel.loadToday()
        assertTrue(viewModel.uiState.value is WorkoutTodayUiState.Unauthorized)
    }

    @Test
    fun retryLoadsAgain() {
        val repository = TestRepository().also { it.result = emptyList() }
        val viewModel = viewModel(repository)

        viewModel.loadToday()
        repository.result = listOf(plan())
        viewModel.retry()

        assertEquals(2, repository.loadCalls)
        assertTrue(viewModel.uiState.value is WorkoutTodayUiState.Success)
    }

    @Test
    fun cancellationIsPropagated() {
        val repository = TestRepository().also {
            it.loader = {
                try {
                    awaitCancellation()
                } catch (exception: CancellationException) {
                    throw exception
                }
            }
        }
        val viewModel = viewModel(repository)

        viewModel.loadToday()
        dateProvider.date = "2026-09-16"
        viewModel.loadToday()

        assertTrue(repository.cancellationObserved)
    }

    @Test
    fun olderResponseCannotOverwriteNewerLoad() {
        val repository = TestRepository()
        val oldResponse = CompletableDeferred<List<WorkoutPlan>>()
        val newResponse = CompletableDeferred<List<WorkoutPlan>>()
        repository.loader = { week ->
            if (week == "2026-09-07") {
                try {
                    oldResponse.await()
                } catch (exception: CancellationException) {
                    withContext(NonCancellable) { oldResponse.await() }
                }
            } else {
                newResponse.await()
            }
        }
        val viewModel = viewModel(repository)

        viewModel.loadToday()
        dateProvider.date = "2026-09-16"
        viewModel.loadToday()
        newResponse.complete(listOf(plan(12L, startDate = "2026-09-01")))
        oldResponse.complete(listOf(plan()))

        val state = viewModel.uiState.value as WorkoutTodayUiState.Success
        assertEquals(12L, state.plan.id)
    }

    private fun viewModel(repository: TestRepository) = WorkoutTodayViewModel(
        getMyWorkoutWeek = GetMyWorkoutWeekUseCase(repository),
        selectPlan = SelectWorkoutPlanForExecutionUseCase(dateProvider),
        createSnapshot = CreateWorkoutExecutionSnapshotUseCase(dateProvider),
        dateProvider = dateProvider,
        scope = CoroutineScope(Dispatchers.Unconfined),
    )

    private fun plan(
        id: Long = 10L,
        startDate: String = "2026-09-01",
    ) = WorkoutPlan(
        id = id,
        clientId = 20L,
        trainerId = 30L,
        sourceTemplateId = null,
        startDate = startDate,
        endDate = "2026-09-30",
        status = "ACTIVE",
        days = listOf(
            WorkoutPlanDay(
                id = id,
                dayOfWeek = 3,
                exercises = listOf(
                    WorkoutPlanExercise(
                        id = id,
                        exercise = Exercise(id, "Press banca", null, 1L, "Pecho", true),
                        sourceTemplateExerciseId = null,
                        orderIndex = 1,
                        sets = 4,
                        repetitions = 8,
                        restSeconds = 90,
                    ),
                ),
            ),
        ),
    )
}

private class FixedDateProvider(
    var date: String,
    private val timestamp: Long,
) : WorkoutExecutionDateProvider {
    override fun todayIsoDate(): String = date

    override fun nowEpochMillis(): Long = timestamp
}

private class TestRepository : WorkoutPlanRepository {
    var result = emptyList<WorkoutPlan>()
    var failure: Exception? = null
    var loader: (suspend (String) -> List<WorkoutPlan>)? = null
    var loadCalls = 0
    var cancellationObserved = false

    override suspend fun getMine() = result

    override suspend fun getMyWeek(weekStart: String): List<WorkoutPlan> {
        loadCalls++
        return try {
            loader?.invoke(weekStart) ?: failure?.let { throw it } ?: result
        } catch (exception: CancellationException) {
            cancellationObserved = true
            throw exception
        }
    }

    override suspend fun create(clientId: Long, request: CreateWorkoutPlanRequest) = error("unused")
    override suspend fun getForClient(clientId: Long) = result
    override suspend fun get(id: Long) = error("unused")
    override suspend fun update(id: Long, request: UpdateWorkoutPlanRequest) = error("unused")
    override suspend fun addDay(planId: Long, request: WorkoutPlanDayRequest) = error("unused")
    override suspend fun deleteDay(planId: Long, day: Int) = Unit
    override suspend fun addExercise(
        planId: Long,
        day: Int,
        request: WorkoutPlanExerciseRequest,
    ) = error("unused")
    override suspend fun updateExercise(id: Long, request: WorkoutPlanExerciseRequest) = error("unused")
    override suspend fun deleteExercise(id: Long) = Unit
    override suspend fun deactivate(id: Long) = Unit
    override suspend fun complete(id: Long) = Unit
}
