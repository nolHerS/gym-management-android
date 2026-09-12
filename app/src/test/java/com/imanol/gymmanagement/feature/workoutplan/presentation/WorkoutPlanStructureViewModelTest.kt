package com.imanol.gymmanagement.feature.workoutplan.presentation

import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.feature.workoutplan.domain.CreateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.GetWorkoutPlanDetailUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.AddWorkoutPlanDayUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.DeleteWorkoutPlanDayUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.AddWorkoutPlanExerciseUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.UpdateWorkoutPlanExerciseUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.DeleteWorkoutPlanExerciseUseCase
import com.imanol.gymmanagement.feature.exercise.domain.GetExerciseCategoriesUseCase
import com.imanol.gymmanagement.feature.exercise.domain.GetExercisesByCategoryUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.UpdateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDay
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExercise
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExerciseRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanRepository
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDayRequest
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
        val viewModel = viewModel(result = { plan })

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
        val viewModel = viewModel(result = {
            calls++
            plan
        })

        viewModel.load(plan.id)
        viewModel.retry()

        assertEquals(2, calls)
    }

    @Test
    fun loadIfNeededUsesSeedPlanWithoutNetworkCall() {
        var calls = 0
        val viewModel = viewModel(result = {
            calls++
            plan
        })

        viewModel.loadIfNeeded(plan.id, plan)

        assertEquals(0, calls)
        assertEquals(
            WorkoutPlanStructureState.Success(plan),
            viewModel.state.value,
        )
    }

    @Test
    fun loadIfNeededSkipsCallWhenAlreadyLoaded() {
        var calls = 0
        val viewModel = viewModel(result = {
            calls++
            plan
        })

        viewModel.load(plan.id)
        viewModel.loadIfNeeded(plan.id)

        assertEquals(1, calls)
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
            val viewModel = viewModel(result = { throw failure })
            viewModel.load(plan.id)
            assertEquals(WorkoutPlanStructureState.Error(message), viewModel.state.value)
        }
    }

    @Test
    fun cancellationIsNotConvertedToError() {
        val viewModel = viewModel(result = { throw CancellationException() })

        viewModel.load(plan.id)

        assertTrue(viewModel.state.value is WorkoutPlanStructureState.Loading)
    }

    @Test
    fun emptyDaysAndExercisesAreRepresented() {
        val emptyPlan = plan.copy(days = listOf(WorkoutPlanDay(1L, 1, emptyList())))
        val viewModel = viewModel(result = { emptyPlan })

        viewModel.load(emptyPlan.id)

        val loaded = (viewModel.state.value as WorkoutPlanStructureState.Success).plan
        assertTrue(loaded.days.single().exercises.isEmpty())
    }

    @Test
    fun addDayUpdatesPlanAndSortsDays() {
        val viewModel = viewModel(
            addDayResult = { request -> WorkoutPlanDay(30L, request.dayOfWeek, request.exercises.map { exercise }) },
        )

        viewModel.load(plan.id)
        viewModel.addDay(
            plan.id,
            WorkoutPlanDayRequest(3, listOf(WorkoutPlanExerciseRequest(exerciseId = 9L, orderIndex = 1, sets = 1, repetitions = 1, restSeconds = 0))),
        )

        val loaded = (viewModel.state.value as WorkoutPlanStructureState.Success).plan
        assertEquals(listOf(1, 3, 5), loaded.days.map { it.dayOfWeek })
        assertTrue(viewModel.mutation.value is WorkoutPlanStructureMutationState.Success)
    }

    @Test
    fun deleteDayUsesOneCallAndUpdatesPlan() {
        var calls = 0
        val viewModel = viewModel(deleteDayResult = { calls++ })

        viewModel.load(plan.id)
        viewModel.deleteDay(plan.id, 5)

        assertEquals(1, calls)
        val loaded = (viewModel.state.value as WorkoutPlanStructureState.Success).plan
        assertEquals(listOf(1), loaded.days.map { it.dayOfWeek })
    }

    @Test
    fun addDayRejectsDuplicateAndInactivePlans() {
        val request = WorkoutPlanDayRequest(
            1,
            listOf(WorkoutPlanExerciseRequest(exerciseId = 9L, orderIndex = 1, sets = 1, repetitions = 1, restSeconds = 0)),
        )
        val duplicate = viewModel()
        duplicate.load(plan.id)
        duplicate.addDay(plan.id, request)
        assertTrue(duplicate.mutation.value is WorkoutPlanStructureMutationState.Error)

        val inactive = viewModel(result = { plan.copy(status = "COMPLETED") })
        inactive.load(plan.id)
        inactive.deleteDay(plan.id, 1)
        assertTrue(inactive.mutation.value is WorkoutPlanStructureMutationState.Error)
    }

    @Test
    fun mutationDoesNotChangePlanOnConflict() {
        val viewModel = viewModel(
            deleteDayResult = { throw AppException.Conflict(IOException()) },
        )
        viewModel.load(plan.id)
        viewModel.deleteDay(plan.id, 5)

        val loaded = (viewModel.state.value as WorkoutPlanStructureState.Success).plan
        assertEquals(listOf(5, 1), loaded.days.map { it.dayOfWeek })
        assertEquals(
            WorkoutPlanStructureMutationState.Error("El plan ha cambiado. Recarga el plan antes de continuar."),
            viewModel.mutation.value,
        )
    }

    @Test
    fun deleteDayIgnoresSecondSubmitWhileDeleting() {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val viewModel = viewModel(deleteDayResult = {
            calls++
            gate.await()
        })
        viewModel.load(plan.id)
        viewModel.deleteDay(plan.id, 5)
        viewModel.deleteDay(plan.id, 1)

        assertEquals(1, calls)
        gate.complete(Unit)
    }

    @Test
    fun cancellationIsNotConvertedToMutationError() {
        val viewModel = viewModel(deleteDayResult = { throw CancellationException() })
        viewModel.load(plan.id)
        viewModel.deleteDay(plan.id, 5)

        assertEquals(WorkoutPlanStructureMutationState.DeletingDay, viewModel.mutation.value)
    }

    @Test
    fun addUpdateAndDeleteExerciseUpdateLocalStructure() {
        val added = exercise.copy(id = 12L, orderIndex = 3)
        val updated = added.copy(sets = 6, orderIndex = 3)
        val viewModel = viewModel(
            addExerciseResult = { _, _, _ -> added },
            updateExerciseResult = { _, _ -> updated },
        )
        viewModel.load(plan.id)

        viewModel.addExercise(plan.id, 5, request(orderIndex = 3))
        var loaded = (viewModel.state.value as WorkoutPlanStructureState.Success).plan
        assertEquals(listOf(2, 3), loaded.days.first { it.dayOfWeek == 5 }.exercises.map { it.orderIndex })

        viewModel.updateExercise(plan.id, 12L, request(orderIndex = 3, sets = 6))
        loaded = (viewModel.state.value as WorkoutPlanStructureState.Success).plan
        assertEquals(6, loaded.days.first { it.dayOfWeek == 5 }.exercises.first { it.id == 12L }.sets)

        viewModel.deleteExercise(plan.id, 12L)
        loaded = (viewModel.state.value as WorkoutPlanStructureState.Success).plan
        assertTrue(loaded.days.first { it.dayOfWeek == 5 }.exercises.none { it.id == 12L })
    }

    @Test
    fun exerciseValidationRejectsDuplicateOrderAndInactivePlan() {
        val viewModel = viewModel()
        viewModel.load(plan.id)
        viewModel.addExercise(plan.id, 5, request(orderIndex = 2))
        assertEquals(
            WorkoutPlanStructureMutationState.Error("El orden ya existe en este día."),
            viewModel.mutation.value,
        )

        val inactive = viewModel(result = { plan.copy(status = "INACTIVE") })
        inactive.load(plan.id)
        inactive.addExercise(plan.id, 5, request(orderIndex = 3))
        assertTrue(inactive.mutation.value is WorkoutPlanStructureMutationState.Error)
    }

    @Test
    fun exerciseConflictDoesNotChangeLocalPlan() {
        val viewModel = viewModel(
            addExerciseResult = { _, _, _ -> throw AppException.Conflict(IOException()) },
        )
        viewModel.load(plan.id)
        viewModel.addExercise(plan.id, 5, request(orderIndex = 3))

        val loaded = (viewModel.state.value as WorkoutPlanStructureState.Success).plan
        assertEquals(1, loaded.days.first { it.dayOfWeek == 5 }.exercises.size)
        assertEquals(
            WorkoutPlanStructureMutationState.Error("El plan ha cambiado. Recarga el plan antes de continuar."),
            viewModel.mutation.value,
        )
    }

    private fun request(orderIndex: Int, sets: Int = 4) = WorkoutPlanExerciseRequest(
        exerciseId = 99L,
        orderIndex = orderIndex,
        sets = sets,
        repetitions = 10,
        restSeconds = 90,
    )

    private fun viewModel(
        result: suspend () -> WorkoutPlan = { plan },
        addDayResult: suspend (WorkoutPlanDayRequest) -> WorkoutPlanDay = {
            WorkoutPlanDay(30L, it.dayOfWeek, it.exercises.map { exercise })
        },
        deleteDayResult: suspend () -> Unit = {},
        addExerciseResult: suspend (Long, Int, WorkoutPlanExerciseRequest) -> WorkoutPlanExercise = { _, _, _ -> exercise },
        updateExerciseResult: suspend (Long, WorkoutPlanExerciseRequest) -> WorkoutPlanExercise = { _, _ -> exercise },
        deleteExerciseResult: suspend (Long) -> Unit = {},
    ): WorkoutPlanStructureViewModel {
        val repository = object : WorkoutPlanRepository {
            override suspend fun getMine() = emptyList<WorkoutPlan>()
            override suspend fun getMyWeek(weekStart: String) = emptyList<WorkoutPlan>()
            override suspend fun create(clientId: Long, request: CreateWorkoutPlanRequest) = plan
            override suspend fun getForClient(clientId: Long) = emptyList<WorkoutPlan>()
            override suspend fun get(id: Long) = result()
            override suspend fun update(id: Long, request: UpdateWorkoutPlanRequest) = plan
            override suspend fun addDay(planId: Long, request: WorkoutPlanDayRequest) = addDayResult(request)
            override suspend fun deleteDay(planId: Long, day: Int) = deleteDayResult()
            override suspend fun addExercise(planId: Long, day: Int, request: WorkoutPlanExerciseRequest) =
                addExerciseResult(planId, day, request)
            override suspend fun updateExercise(id: Long, request: WorkoutPlanExerciseRequest) =
                updateExerciseResult(id, request)
            override suspend fun deleteExercise(id: Long) = deleteExerciseResult(id)
            override suspend fun deactivate(id: Long) = Unit
            override suspend fun complete(id: Long) = Unit
        }
        return WorkoutPlanStructureViewModel(
            GetWorkoutPlanDetailUseCase(repository),
            AddWorkoutPlanDayUseCase(repository),
            DeleteWorkoutPlanDayUseCase(repository),
            AddWorkoutPlanExerciseUseCase(repository),
            UpdateWorkoutPlanExerciseUseCase(repository),
            DeleteWorkoutPlanExerciseUseCase(repository),
            GetExerciseCategoriesUseCase(object : com.imanol.gymmanagement.feature.exercise.domain.ExerciseRepository {
                override suspend fun getExerciseCategories() = emptyList<com.imanol.gymmanagement.feature.exercise.domain.ExerciseCategory>()
                override suspend fun getExercisesByCategory(categoryId: Long) = emptyList<com.imanol.gymmanagement.feature.exercise.domain.Exercise>()
                override suspend fun getExerciseById(exerciseId: Long) = error("unused")
            }),
            GetExercisesByCategoryUseCase(object : com.imanol.gymmanagement.feature.exercise.domain.ExerciseRepository {
                override suspend fun getExerciseCategories() = emptyList<com.imanol.gymmanagement.feature.exercise.domain.ExerciseCategory>()
                override suspend fun getExercisesByCategory(categoryId: Long) = emptyList<com.imanol.gymmanagement.feature.exercise.domain.Exercise>()
                override suspend fun getExerciseById(exerciseId: Long) = error("unused")
            }),
            CoroutineScope(Dispatchers.Unconfined),
        )
    }
}
