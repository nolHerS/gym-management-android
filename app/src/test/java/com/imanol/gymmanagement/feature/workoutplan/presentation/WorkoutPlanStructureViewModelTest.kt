package com.imanol.gymmanagement.feature.workoutplan.presentation

import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.feature.workoutplan.domain.CreateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.GetWorkoutPlanDetailUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.UpdateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDay
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExercise
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExerciseRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPlanStructureViewModelTest {
    private val exercise = WorkoutPlanExercise(
        id = 11L,
        exercise = null,
        sourceTemplateExerciseId = 7L,
        orderIndex = 2,
        sets = 4,
        repetitions = 10,
        restSeconds = 90,
    )
    private val plan = WorkoutPlan(
        id = 8L,
        clientId = 2L,
        trainerId = 1L,
        sourceTemplateId = null,
        startDate = "2026-09-01",
        endDate = null,
        status = "ACTIVE",
        days = listOf(
            WorkoutPlanDay(20L, 5, listOf(exercise)),
            WorkoutPlanDay(19L, 1, emptyList()),
        ),
    )

    @Test
    fun loadEmitsSuccessWithCompleteStructure() {
        val viewModel = viewModel { plan }

        viewModel.load(plan.id)

        val state = viewModel.state.value as WorkoutPlanStructureState.Success
        assertEquals(plan, state.plan)
        assertEquals(2, state.plan.days.size)
        assertEquals(4, state.plan.days[0].exercises.single().sets)
        assertEquals(10, state.plan.days[0].exercises.single().repetitions)
        assertEquals(90, state.plan.days[0].exercises.single().restSeconds)
        assertEquals(2, state.plan.days[0].exercises.single().orderIndex)
    }

    @Test
    fun retryReloadsTheLastPlanId() {
        var calls = 0
        val viewModel = viewModel {
            calls++
            plan
        }

        viewModel.load(plan.id)
        viewModel.retry()

        assertEquals(2, calls)
    }

    @Test
    fun mapsAllSemanticErrors() {
        val failures = listOf(
            AppException.BadRequest(IOException()) to "Los datos del plan no son válidos.",
            AppException.Forbidden(IOException()) to "No tienes permisos para consultar este plan.",
            AppException.NotFound(IOException()) to "El plan no existe.",
            AppException.Conflict(IOException()) to "El plan ha cambiado. Recarga el plan antes de continuar.",
            AppException.Server(IOException()) to "El servidor no está disponible. Inténtalo de nuevo.",
            AppException.Network(IOException()) to "No se ha podido conectar con el servidor.",
            AppException.Serialization(IOException()) to "La respuesta del servidor no es válida.",
            AppException.Unexpected(IOException()) to "Ha ocurrido un error inesperado.",
        )

        failures.forEach { (failure, message) ->
            val viewModel = viewModel { throw failure }
            viewModel.load(plan.id)
            assertEquals(WorkoutPlanStructureState.Error(message), viewModel.state.value)
        }
    }

    @Test
    fun cancellationIsNotConvertedToError() {
        val viewModel = viewModel { throw CancellationException() }

        viewModel.load(plan.id)

        assertTrue(viewModel.state.value is WorkoutPlanStructureState.Loading)
    }

    @Test
    fun emptyDaysAndExercisesAreRepresented() {
        val emptyPlan = plan.copy(days = listOf(WorkoutPlanDay(1L, 1, emptyList())))
        val viewModel = viewModel { emptyPlan }

        viewModel.load(emptyPlan.id)

        val loaded = (viewModel.state.value as WorkoutPlanStructureState.Success).plan
        assertTrue(loaded.days.single().exercises.isEmpty())
    }

    private fun viewModel(result: suspend () -> WorkoutPlan): WorkoutPlanStructureViewModel {
        val repository = object : WorkoutPlanRepository {
            override suspend fun getMine() = emptyList<WorkoutPlan>()
            override suspend fun getMyWeek(weekStart: String) = emptyList<WorkoutPlan>()
            override suspend fun create(clientId: Long, request: CreateWorkoutPlanRequest) = plan
            override suspend fun getForClient(clientId: Long) = emptyList<WorkoutPlan>()
            override suspend fun get(id: Long) = result()
            override suspend fun update(id: Long, request: UpdateWorkoutPlanRequest) = plan
            override suspend fun addDay(planId: Long, request: com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDayRequest) =
                plan.days.first()
            override suspend fun deleteDay(planId: Long, day: Int) = Unit
            override suspend fun addExercise(planId: Long, day: Int, request: WorkoutPlanExerciseRequest) =
                exercise
            override suspend fun updateExercise(id: Long, request: WorkoutPlanExerciseRequest) = exercise
            override suspend fun deleteExercise(id: Long) = Unit
            override suspend fun deactivate(id: Long) = Unit
            override suspend fun complete(id: Long) = Unit
        }
        return WorkoutPlanStructureViewModel(
            GetWorkoutPlanDetailUseCase(repository),
            CoroutineScope(Dispatchers.Unconfined),
        )
    }
}
