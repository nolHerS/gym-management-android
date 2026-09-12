package com.imanol.gymmanagement.feature.workoutplan.presentation

import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseCategory
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseRepository
import com.imanol.gymmanagement.feature.exercise.domain.GetExerciseCategoriesUseCase
import com.imanol.gymmanagement.feature.exercise.domain.GetExercisesByCategoryUseCase
import com.imanol.gymmanagement.feature.workout.domain.GetWorkoutTemplateDetailUseCase
import com.imanol.gymmanagement.feature.workout.domain.GetWorkoutTemplatesUseCase
import com.imanol.gymmanagement.feature.workout.domain.WorkoutRepository
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplate
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplateDetail
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplateExercise
import com.imanol.gymmanagement.feature.workoutplan.domain.CompleteWorkoutPlanUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.CreateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.CreateWorkoutPlanUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.DeactivateWorkoutPlanUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.GetClientWorkoutPlansUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.GetWorkoutPlanDetailUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.UpdateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.UpdateWorkoutPlanUseCase
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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun createFromScratchBuildsSingleRequestAndCachesDetail() {
        var createCalls = 0
        var captured: CreateWorkoutPlanRequest? = null
        val created = plan.copy(
            id = 9L,
            days = listOf(
                WorkoutPlanDay(
                    id = 91L,
                    dayOfWeek = 3,
                    exercises = listOf(
                        WorkoutPlanExercise(
                            id = 100L,
                            exercise = Exercise(6L, "Press", null, 2L, "Chest", true),
                            sourceTemplateExerciseId = null,
                            orderIndex = 1,
                            sets = 4,
                            repetitions = 10,
                            restSeconds = 60,
                        ),
                    ),
                ),
            ),
        )
        val viewModel = viewModel(createResult = { request ->
            createCalls++
            captured = request
            created
        })

        var savedId: Long? = null
        viewModel.prepareCreate()
        viewModel.selectFromScratch()
        viewModel.addDraftDay(3)
        viewModel.upsertDraftExercise(
            dayOfWeek = 3,
            request = WorkoutPlanExerciseRequest(
                exerciseId = 6L,
                orderIndex = 1,
                sets = 4,
                repetitions = 10,
                restSeconds = 60,
            ),
            exerciseName = "Press",
        )
        viewModel.setStartDate("2026-09-01")
        viewModel.create(2L) { savedId = it }

        assertEquals(1, createCalls)
        assertEquals(9L, savedId)
        assertEquals(9L, viewModel.createState.value.createdId)
        assertEquals(created, (viewModel.detail.value as WorkoutPlanDetailState.Success).plan)
        assertNull(captured?.sourceTemplateId)
        assertEquals(1, captured?.days?.size)
        assertEquals(3, captured?.days?.single()?.dayOfWeek)
    }

    @Test
    fun createFromScratchRetryPreservesDraftAfterError() {
        var calls = 0
        val viewModel = viewModel(createResult = {
            calls++
            if (calls == 1) throw AppException.Network(IOException())
            plan.copy(id = 50L)
        })

        var saved: Long? = null
        viewModel.prepareCreate()
        viewModel.selectFromScratch()
        viewModel.addDraftDay(2)
        viewModel.upsertDraftExercise(
            dayOfWeek = 2,
            request = WorkoutPlanExerciseRequest(
                exerciseId = 11L,
                orderIndex = 1,
                sets = 3,
                repetitions = 8,
                restSeconds = 45,
            ),
            exerciseName = "Row",
        )
        viewModel.setStartDate("2026-09-01")

        viewModel.create(2L) { saved = it }
        assertEquals("No se pudo conectar con el servidor.", viewModel.createState.value.error)
        assertEquals(1, viewModel.createState.value.scratchDraft.days.size)
        assertEquals(1, viewModel.createState.value.scratchDraft.days.single().exercises.size)

        viewModel.retryCreate(2L) { saved = it }

        assertEquals(2, calls)
        assertEquals(50L, saved)
        assertTrue(viewModel.createState.value.saved)
    }

    @Test
    fun createFromScratchRejectsInvalidDraftBeforePosting() {
        var createCalls = 0
        val viewModel = viewModel(createResult = {
            createCalls++
            plan
        })

        viewModel.prepareCreate()
        viewModel.selectFromScratch()
        viewModel.addDraftDay(4)
        viewModel.setStartDate("2026-09-01")
        viewModel.create(2L) {}

        assertEquals(0, createCalls)
        assertEquals("Añade al menos un ejercicio a cada día.", viewModel.createState.value.error)
    }

    @Test
    fun createIgnoresSecondSubmitWhileSaving() {
        val gate = CompletableDeferred<WorkoutPlan>()
        var calls = 0
        val viewModel = viewModel(createResult = {
            calls++
            gate.await()
        })

        viewModel.prepareCreate()
        viewModel.selectFromScratch()
        viewModel.addDraftDay(1)
        viewModel.upsertDraftExercise(
            dayOfWeek = 1,
            request = WorkoutPlanExerciseRequest(
                exerciseId = 8L,
                orderIndex = 1,
                sets = 3,
                repetitions = 8,
                restSeconds = 45,
            ),
            exerciseName = "Curl",
        )
        viewModel.setStartDate("2026-09-01")

        viewModel.create(2L) {}
        viewModel.create(2L) {}

        assertEquals(1, calls)
        gate.complete(plan)
    }

    @Test
    fun createCancellationDoesNotSetErrorAndStopsSaving() {
        val viewModel = viewModel(createResult = { throw CancellationException() })

        viewModel.prepareCreate()
        viewModel.selectFromScratch()
        viewModel.addDraftDay(1)
        viewModel.upsertDraftExercise(
            dayOfWeek = 1,
            request = WorkoutPlanExerciseRequest(
                exerciseId = 8L,
                orderIndex = 1,
                sets = 3,
                repetitions = 8,
                restSeconds = 45,
            ),
            exerciseName = "Curl",
        )
        viewModel.setStartDate("2026-09-01")
        viewModel.create(2L) {}

        assertNull(viewModel.createState.value.error)
        assertTrue(!viewModel.createState.value.saving)
    }

    @Test
    fun templateCreationStillUsesTemplatePayload() {
        val template = WorkoutTemplate(99L, "Template A", null, true)
        val templateExercise = WorkoutTemplateExercise(
            id = 100L,
            exercise = Exercise(7L, "Sentadilla", null, 4L, "Legs", true),
            orderIndex = 1,
            sets = 5,
            repetitions = 5,
            restSeconds = 120,
        )
        var captured: CreateWorkoutPlanRequest? = null
        val viewModel = viewModel(
            templatesResult = { listOf(template) },
            templateDetailResult = {
                WorkoutTemplateDetail(template, listOf(templateExercise))
            },
            createResult = {
                captured = it
                plan.copy(id = 80L)
            },
        )

        viewModel.prepareCreate()
        viewModel.selectTemplate(template)
        viewModel.assignExercise(templateExercise.id, 4)
        viewModel.setStartDate("2026-09-01")
        viewModel.create(2L) {}

        assertEquals(99L, captured?.sourceTemplateId)
        assertEquals(4, captured?.days?.single()?.dayOfWeek)
        assertEquals(7L, captured?.days?.single()?.exercises?.single()?.exerciseId)
    }

    @Test
    fun selectingNewTemplateClearsPreviousStructureAndBlocksCreateUntilLoaded() {
        val templateA = WorkoutTemplate(101L, "A", null, true)
        val templateB = WorkoutTemplate(102L, "B", null, true)
        val detailA = WorkoutTemplateDetail(templateA, emptyList())
        val detailBGate = CompletableDeferred<WorkoutTemplateDetail>()
        var createCalls = 0
        val viewModel = viewModel(
            templateDetailResult = { id ->
                if (id == templateB.id) detailBGate.await() else detailA
            },
            createResult = {
                createCalls++
                plan
            },
        )

        viewModel.prepareCreate()
        viewModel.selectTemplate(templateA)
        viewModel.selectTemplate(templateB)
        viewModel.create(2L) {}

        assertEquals(0, createCalls)
        assertTrue(viewModel.createState.value.assignments.isEmpty())
        assertTrue(viewModel.createState.value.drafts.isEmpty())
        assertTrue(viewModel.createState.value.templateDetailLoading)

        detailBGate.complete(WorkoutTemplateDetail(templateB, emptyList()))
        assertEquals(templateB.id, viewModel.createState.value.template?.id)
    }

    @Test
    fun staleTemplateResponseCannotOverwriteCurrentSelection() {
        val templateA = WorkoutTemplate(201L, "A", null, true)
        val templateB = WorkoutTemplate(202L, "B", null, true)
        val detailAGate = CompletableDeferred<WorkoutTemplateDetail>()
        val detailBGate = CompletableDeferred<WorkoutTemplateDetail>()
        val viewModel = viewModel(
            templateDetailResult = { id ->
                when (id) {
                    templateA.id -> detailAGate.await()
                    else -> detailBGate.await()
                }
            },
        )

        viewModel.prepareCreate()
        viewModel.selectTemplate(templateA)
        viewModel.selectTemplate(templateB)
        detailBGate.complete(WorkoutTemplateDetail(templateB, emptyList()))
        detailAGate.complete(WorkoutTemplateDetail(templateA, emptyList()))

        assertEquals(templateB.id, viewModel.createState.value.templateDetail?.template?.id)
    }

    @Test
    fun fromScratchRejectsTemplateExerciseReference() {
        val viewModel = viewModel()

        viewModel.prepareCreate()
        viewModel.selectFromScratch()
        val added = viewModel.upsertDraftExercise(
            dayOfWeek = 1,
            request = WorkoutPlanExerciseRequest(
                sourceTemplateExerciseId = 9L,
                orderIndex = 1,
                sets = 3,
                repetitions = 8,
                restSeconds = 45,
            ),
        )

        assertTrue(!added)
        assertTrue(viewModel.createState.value.scratchDraft.days.singleOrNull() == null)
    }

    @Test
    fun draftOperationsAddUpdateDeleteDaysAndExercises() {
        val viewModel = viewModel(result = { emptyList() })

        viewModel.prepareCreate()
        viewModel.selectFromScratch()
        viewModel.addDraftDay(3)
        viewModel.upsertDraftExercise(
            dayOfWeek = 3,
            request = WorkoutPlanExerciseRequest(
                exerciseId = 10L,
                orderIndex = 1,
                sets = 4,
                repetitions = 12,
                restSeconds = 30,
            ),
            exerciseName = "Press",
        )

        val localId = viewModel.createState.value.scratchDraft.days.single().exercises.single().localId
        val dayLocalId = viewModel.createState.value.scratchDraft.days.single().localId

        viewModel.upsertDraftExercise(
            dayOfWeek = 3,
            dayLocalId = dayLocalId,
            request = WorkoutPlanExerciseRequest(
                exerciseId = 10L,
                orderIndex = 1,
                sets = 6,
                repetitions = 12,
                restSeconds = 30,
            ),
            exerciseName = "Press",
            localId = localId,
        )
        assertEquals(6, viewModel.createState.value.scratchDraft.days.single().exercises.single().request.sets)

        viewModel.deleteDraftExercise(localId)
        assertTrue(viewModel.createState.value.scratchDraft.days.single().exercises.isEmpty())

        viewModel.deleteDraftDay(dayLocalId)
        assertTrue(viewModel.createState.value.scratchDraft.days.isEmpty())
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

    @Test
    fun completeUpdatesStateAndPreventsSecondSubmit() {
        var calls = 0
        val gate = CompletableDeferred<Unit>()
        val viewModel = viewModel(
            completeResult = {
                calls++
                gate.await()
            },
        )
        viewModel.loadDetail(plan.id)
        viewModel.complete(plan.id)
        viewModel.complete(plan.id)

        assertEquals(1, calls)
        assertEquals(WorkoutPlanMutationState.Completing, viewModel.mutation.value)
        gate.complete(Unit)
        assertEquals("COMPLETED", (viewModel.detail.value as WorkoutPlanDetailState.Success).plan.status)
    }

    @Test
    fun deactivateUpdatesState() {
        val viewModel = viewModel(deactivateResult = { Unit })
        viewModel.loadDetail(plan.id)
        viewModel.deactivate(plan.id)

        assertEquals("INACTIVE", (viewModel.detail.value as WorkoutPlanDetailState.Success).plan.status)
        assertEquals(
            WorkoutPlanMutationState.Success("Plan desactivado correctamente."),
            viewModel.mutation.value,
        )
    }

    @Test
    fun lifecycleMutationMapsConflict() {
        val viewModel = viewModel(completeResult = { throw AppException.Conflict(IOException()) })
        viewModel.loadDetail(plan.id)
        viewModel.complete(plan.id)

        assertEquals(
            WorkoutPlanMutationState.Error(
                "El plan ha cambiado o existe un conflicto. Vuelve a cargarlo antes de continuar.",
            ),
            viewModel.mutation.value,
        )
    }

    @Test
    fun lifecycleMutationMapsAllErrors() {
        val failures = listOf(
            AppException.BadRequest(IOException()) to "El plan no puede cambiar a este estado.",
            AppException.Forbidden(IOException()) to "No tienes permisos para modificar este plan.",
            AppException.NotFound(IOException()) to "El plan ya no existe.",
            AppException.Conflict(IOException()) to "El plan ha cambiado o existe un conflicto. Vuelve a cargarlo antes de continuar.",
            AppException.Server(IOException()) to "El servidor no está disponible. Inténtalo de nuevo.",
            AppException.Network(IOException()) to "No se ha podido conectar con el servidor.",
            AppException.Serialization(IOException()) to "La respuesta del servidor no es válida.",
            AppException.Unexpected(IOException()) to "Ha ocurrido un error inesperado.",
        )

        failures.forEach { (failure, message) ->
            val completeViewModel = viewModel(completeResult = { throw failure })
            completeViewModel.loadDetail(plan.id)
            completeViewModel.complete(plan.id)
            assertEquals(WorkoutPlanMutationState.Error(message), completeViewModel.mutation.value)

            val deactivateViewModel = viewModel(deactivateResult = { throw failure })
            deactivateViewModel.loadDetail(plan.id)
            deactivateViewModel.deactivate(plan.id)
            assertEquals(WorkoutPlanMutationState.Error(message), deactivateViewModel.mutation.value)
        }
    }

    @Test
    fun lifecycleMutationDoesNotConvertCancellationToError() {
        val viewModel = viewModel(completeResult = { throw CancellationException() })
        viewModel.loadDetail(plan.id)
        viewModel.complete(plan.id)

        assertEquals(WorkoutPlanMutationState.Completing, viewModel.mutation.value)
    }

    private fun viewModel(
        result: suspend () -> List<WorkoutPlan> = { emptyList() },
        planResult: suspend () -> WorkoutPlan = { plan },
        createResult: suspend (CreateWorkoutPlanRequest) -> WorkoutPlan = { plan },
        updateResult: suspend (UpdateWorkoutPlanRequest) -> WorkoutPlan = { plan },
        completeResult: suspend () -> Unit = {},
        deactivateResult: suspend () -> Unit = {},
        templatesResult: suspend () -> List<WorkoutTemplate> = { emptyList() },
        templateDetailResult: suspend (Long) -> WorkoutTemplateDetail = {
            WorkoutTemplateDetail(WorkoutTemplate(it, "Template", null, true), emptyList())
        },
        categoriesResult: suspend () -> List<ExerciseCategory> = { emptyList() },
        exercisesResult: suspend (Long) -> List<Exercise> = { emptyList() },
    ): WorkoutPlanViewModel {
        val repository = object : WorkoutPlanRepository {
            override suspend fun getMine() = emptyList<WorkoutPlan>()
            override suspend fun getMyWeek(weekStart: String) = emptyList<WorkoutPlan>()
            override suspend fun create(clientId: Long, request: CreateWorkoutPlanRequest) = createResult(request)
            override suspend fun getForClient(clientId: Long) = result()
            override suspend fun get(id: Long) = planResult()
            override suspend fun update(id: Long, request: UpdateWorkoutPlanRequest) = updateResult(request)
            override suspend fun addDay(planId: Long, request: WorkoutPlanDayRequest) =
                WorkoutPlanDay(1L, request.dayOfWeek, emptyList())

            override suspend fun deleteDay(planId: Long, day: Int) = Unit
            override suspend fun addExercise(planId: Long, day: Int, request: WorkoutPlanExerciseRequest) =
                WorkoutPlanExercise(
                    1L,
                    null,
                    request.sourceTemplateExerciseId,
                    request.orderIndex,
                    request.sets,
                    request.repetitions,
                    request.restSeconds,
                )

            override suspend fun updateExercise(id: Long, request: WorkoutPlanExerciseRequest) =
                WorkoutPlanExercise(
                    id,
                    null,
                    request.sourceTemplateExerciseId,
                    request.orderIndex,
                    request.sets,
                    request.repetitions,
                    request.restSeconds,
                )

            override suspend fun deleteExercise(id: Long) = Unit
            override suspend fun complete(id: Long) = completeResult()
            override suspend fun deactivate(id: Long) = deactivateResult()
        }
        val workoutRepository = object : WorkoutRepository {
            override suspend fun getWorkoutTemplates() = templatesResult()
            override suspend fun getWorkoutTemplateDetail(templateId: Long) = templateDetailResult(templateId)
            override suspend fun createWorkoutTemplate(name: String, description: String?) = error("unused")
            override suspend fun updateWorkoutTemplate(templateId: Long, name: String, description: String?) = error("unused")
            override suspend fun activateWorkoutTemplate(templateId: Long) = Unit
            override suspend fun deactivateWorkoutTemplate(templateId: Long) = Unit
            override suspend fun getWorkoutTemplateExercises(templateId: Long) = emptyList<WorkoutTemplateExercise>()
            override suspend fun addExerciseToWorkoutTemplate(templateId: Long, exerciseId: Long, orderIndex: Int, sets: Int, repetitions: Int, restSeconds: Int) =
                error("unused")

            override suspend fun updateWorkoutTemplateExercise(templateExerciseId: Long, exerciseId: Long, orderIndex: Int, sets: Int, repetitions: Int, restSeconds: Int) =
                error("unused")

            override suspend fun deleteWorkoutTemplateExercise(templateExerciseId: Long) = Unit
        }
        val exerciseRepository = object : ExerciseRepository {
            override suspend fun getExerciseCategories() = categoriesResult()
            override suspend fun getExercisesByCategory(categoryId: Long) = exercisesResult(categoryId)
            override suspend fun getExerciseById(exerciseId: Long) = error("unused")
        }

        return WorkoutPlanViewModel(
            GetClientWorkoutPlansUseCase(repository),
            GetWorkoutPlanDetailUseCase(repository),
            CreateWorkoutPlanUseCase(repository),
            UpdateWorkoutPlanUseCase(repository),
            CompleteWorkoutPlanUseCase(repository),
            DeactivateWorkoutPlanUseCase(repository),
            GetWorkoutTemplatesUseCase(workoutRepository),
            GetWorkoutTemplateDetailUseCase(workoutRepository),
            GetExerciseCategoriesUseCase(exerciseRepository),
            GetExercisesByCategoryUseCase(exerciseRepository),
            CoroutineScope(Dispatchers.Unconfined),
        )
    }
}
