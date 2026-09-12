package com.imanol.gymmanagement.feature.workoutplan.presentation

import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.feature.workout.domain.GetWorkoutTemplateDetailUseCase
import com.imanol.gymmanagement.feature.workout.domain.GetWorkoutTemplatesUseCase
import com.imanol.gymmanagement.feature.workout.domain.WorkoutRepository
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplate
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplateDetail
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplateExercise
import com.imanol.gymmanagement.feature.workoutplan.domain.*
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
        assertEquals(WorkoutPlansState.Empty, viewModel(result = { emptyList() }).also { it.load(2L) }.plans.value)
        assertTrue(viewModel(result = { throw IOException() }).also { it.load(2L) }.plans.value is WorkoutPlansState.Error)
        assertEquals(
            WorkoutPlansState.Unauthorized,
            viewModel(result = { throw HttpException(Response.error<Unit>(401, "Unauthorized".toResponseBody())) })
                .also { it.load(2L) }
                .plans.value,
        )
    }

    @Test
    fun listConflictProducesErrorState() {
        val viewModel = viewModel(result = {
            throw HttpException(Response.error<Unit>(409, "Conflict".toResponseBody()))
        })

        viewModel.load(2L)

        assertTrue(viewModel.plans.value is WorkoutPlansState.Error)
        assertEquals(
            "El recurso ha cambiado o existe un conflicto. Vuelve a cargar e inténtalo de nuevo.",
            (viewModel.plans.value as WorkoutPlansState.Error).message,
        )
    }

    @Test
    fun createViewModelRejectsMissingDatesAndExercises() {
        val viewModel = viewModel(result = { emptyList() })
        viewModel.prepareCreate()
        viewModel.create(2L) {}
        assertTrue(viewModel.createState.value.error != null)
    }

    @Test
    fun editLoadsExistingPlanAndUpdatesIt() {
        val updated = plan.copy(startDate = "2026-09-03", endDate = "2026-09-30")
        val viewModel = viewModel(
            planResult = { plan },
            updateResult = { request ->
                assertEquals("2026-09-03", request.startDate)
                updated
            },
        )

        viewModel.prepareEdit(plan.id)
        assertEquals("2026-09-01", viewModel.createState.value.startDate)
        viewModel.setStartDate("2026-09-03")
        viewModel.update {}

        assertTrue(viewModel.createState.value.saved)
        assertEquals(updated, (viewModel.detail.value as WorkoutPlanDetailState.Success).plan)
    }

    @Test
    fun editMapsSemanticErrors() {
        val cases = listOf(
            AppException.BadRequest(IOException()) to "El servidor rechazó los datos enviados. Revisa la información e inténtalo de nuevo.",
            AppException.Forbidden(IOException()) to "No tienes permisos para modificar este plan.",
            AppException.NotFound(IOException()) to "El plan ya no existe.",
            AppException.Conflict(IOException()) to "El plan ha cambiado o existe un conflicto. Vuelve a cargarlo antes de continuar.",
            AppException.Server(IOException()) to "Se ha producido un error en el servidor.",
            AppException.Network(IOException()) to "No se pudo conectar con el servidor.",
            AppException.Serialization(IOException()) to "No se pudo interpretar la respuesta del servidor.",
        )

        cases.forEach { (failure, message) ->
            val viewModel = viewModel(updateResult = { throw failure })
            viewModel.prepareEdit(plan.id)
            viewModel.update {}
            assertEquals(message, viewModel.createState.value.error)
        }
    }

    @Test
    fun editRethrowsCancellation() {
        val viewModel = viewModel(planResult = { throw CancellationException() })

        viewModel.prepareEdit(plan.id)
        assertTrue(viewModel.createState.value.loading)
        assertEquals(null, viewModel.createState.value.error)
    }

    @Test
    fun editIgnoresSecondSubmitWhileSaving() {
        val gate = CompletableDeferred<WorkoutPlan>()
        var calls = 0
        val viewModel = viewModel(
            updateResult = {
                calls++
                gate.await()
            },
        )
        viewModel.prepareEdit(plan.id)
        viewModel.update {}
        viewModel.update {}

        assertEquals(1, calls)
        gate.complete(plan)
    }

    private fun viewModel(
        result: suspend () -> List<WorkoutPlan> = { emptyList() },
        planResult: suspend () -> WorkoutPlan = { plan },
        updateResult: suspend (UpdateWorkoutPlanRequest) -> WorkoutPlan = { plan },
    ): WorkoutPlanViewModel {
        val repository = object : WorkoutPlanRepository {
            override suspend fun getMine() = emptyList<WorkoutPlan>()
            override suspend fun getMyWeek(weekStart: String) = emptyList<WorkoutPlan>()
            override suspend fun create(clientId: Long, request: CreateWorkoutPlanRequest) = plan
            override suspend fun getForClient(clientId: Long) = result()
            override suspend fun get(id: Long) = planResult()
            override suspend fun update(id: Long, request: UpdateWorkoutPlanRequest) = updateResult(request)
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
            UpdateWorkoutPlanUseCase(repository),
            GetWorkoutTemplatesUseCase(workoutRepository),
            GetWorkoutTemplateDetailUseCase(workoutRepository),
            CoroutineScope(Dispatchers.Unconfined),
        )
    }
}
