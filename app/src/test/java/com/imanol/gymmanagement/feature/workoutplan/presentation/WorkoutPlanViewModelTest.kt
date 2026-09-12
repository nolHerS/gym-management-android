package com.imanol.gymmanagement.feature.workoutplan.presentation

import com.imanol.gymmanagement.feature.workout.domain.GetWorkoutTemplateDetailUseCase
import com.imanol.gymmanagement.feature.workout.domain.GetWorkoutTemplatesUseCase
import com.imanol.gymmanagement.feature.workout.domain.WorkoutRepository
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplate
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplateDetail
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplateExercise
import com.imanol.gymmanagement.feature.workoutplan.domain.*
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class WorkoutPlanViewModelTest {
    private val plan = WorkoutPlan(1L, 2L, 3L, null, "2026-09-01", null, "ACTIVE", emptyList())

    @Test
    fun listHandlesEmptyErrorAndUnauthorized() {
        assertEquals(WorkoutPlansState.Empty, viewModel { emptyList() }.also { it.load(2L) }.plans.value)
        assertTrue(viewModel { throw IOException() }.also { it.load(2L) }.plans.value is WorkoutPlansState.Error)
        assertEquals(
            WorkoutPlansState.Unauthorized,
            viewModel { throw HttpException(Response.error<Unit>(401, "Unauthorized".toResponseBody())) }
                .also { it.load(2L) }
                .plans.value,
        )
    }

    @Test
    fun listConflictProducesErrorState() {
        val viewModel = viewModel {
            throw HttpException(Response.error<Unit>(409, "Conflict".toResponseBody()))
        }

        viewModel.load(2L)

        assertTrue(viewModel.plans.value is WorkoutPlansState.Error)
        assertEquals(
            "El recurso ha cambiado o existe un conflicto. Vuelve a cargar e inténtalo de nuevo.",
            (viewModel.plans.value as WorkoutPlansState.Error).message,
        )
    }

    @Test
    fun createViewModelRejectsMissingDatesAndExercises() {
        val viewModel = viewModel { emptyList() }
        viewModel.prepareCreate()
        viewModel.create(2L) {}
        assertTrue(viewModel.createState.value.error != null)
    }

    private fun viewModel(result: suspend () -> List<WorkoutPlan>): WorkoutPlanViewModel {
        val repository = object : WorkoutPlanRepository {
            override suspend fun getMine() = emptyList<WorkoutPlan>()
            override suspend fun getMyWeek(weekStart: String) = emptyList<WorkoutPlan>()
            override suspend fun create(clientId: Long, request: CreateWorkoutPlanRequest) = plan
            override suspend fun getForClient(clientId: Long) = result()
            override suspend fun get(id: Long) = plan
            override suspend fun update(id: Long, request: UpdateWorkoutPlanRequest) = plan
            override suspend fun addDay(planId: Long, request: WorkoutPlanDayRequest) =
                WorkoutPlanDay(1L, request.dayOfWeek, emptyList())
            override suspend fun deleteDay(planId: Long, day: Int) = Unit
            override suspend fun addExercise(planId: Long, day: Int, request: WorkoutPlanExerciseRequest) =
                WorkoutPlanExercise(1L, null, request.sourceTemplateExerciseId, request.orderIndex, request.sets, request.repetitions, request.restSeconds)
            override suspend fun updateExercise(id: Long, request: WorkoutPlanExerciseRequest) =
                WorkoutPlanExercise(id, null, request.sourceTemplateExerciseId, request.orderIndex, request.sets, request.repetitions, request.restSeconds)
            override suspend fun deleteExercise(id: Long) = Unit
            override suspend fun deactivate(id: Long) = Unit
            override suspend fun complete(id: Long) = Unit
        }
        val workoutRepository = object : WorkoutRepository {
            override suspend fun getWorkoutTemplates() = emptyList<WorkoutTemplate>()
            override suspend fun getWorkoutTemplateDetail(templateId: Long) =
                WorkoutTemplateDetail(WorkoutTemplate(1L, "Template", null, true), emptyList())
            override suspend fun createWorkoutTemplate(name: String, description: String?) =
                error("unused")
            override suspend fun updateWorkoutTemplate(templateId: Long, name: String, description: String?) =
                error("unused")
            override suspend fun activateWorkoutTemplate(templateId: Long) = Unit
            override suspend fun deactivateWorkoutTemplate(templateId: Long) = Unit
            override suspend fun getWorkoutTemplateExercises(templateId: Long) = emptyList<WorkoutTemplateExercise>()
            override suspend fun addExerciseToWorkoutTemplate(templateId: Long, exerciseId: Long, orderIndex: Int, sets: Int, repetitions: Int, restSeconds: Int) =
                error("unused")
            override suspend fun updateWorkoutTemplateExercise(templateExerciseId: Long, exerciseId: Long, orderIndex: Int, sets: Int, repetitions: Int, restSeconds: Int) =
                error("unused")
            override suspend fun deleteWorkoutTemplateExercise(templateExerciseId: Long) = Unit
        }
        return WorkoutPlanViewModel(
            GetClientWorkoutPlansUseCase(repository),
            GetWorkoutPlanDetailUseCase(repository),
            CreateWorkoutPlanUseCase(repository),
            GetWorkoutTemplatesUseCase(workoutRepository),
            GetWorkoutTemplateDetailUseCase(workoutRepository),
            CoroutineScope(Dispatchers.Unconfined),
        )
    }
}
