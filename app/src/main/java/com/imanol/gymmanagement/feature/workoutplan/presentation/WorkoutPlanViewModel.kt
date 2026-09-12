package com.imanol.gymmanagement.feature.workoutplan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.core.network.toAppException
import com.imanol.gymmanagement.feature.exercise.domain.GetExerciseCategoriesUseCase
import com.imanol.gymmanagement.feature.exercise.domain.GetExercisesByCategoryUseCase
import com.imanol.gymmanagement.feature.workout.domain.GetWorkoutTemplateDetailUseCase
import com.imanol.gymmanagement.feature.workout.domain.GetWorkoutTemplatesUseCase
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplate
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplateDetail
import com.imanol.gymmanagement.feature.workoutplan.domain.CompleteWorkoutPlanUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.CreateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.CreateWorkoutPlanUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.CreationMode
import com.imanol.gymmanagement.feature.workoutplan.domain.DeactivateWorkoutPlanUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.GetClientWorkoutPlansUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.GetWorkoutPlanDetailUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.UpdateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.UpdateWorkoutPlanUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDayRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDraft
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDraftExercise
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExerciseRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface WorkoutPlansState {
    data object Loading : WorkoutPlansState
    data object Empty : WorkoutPlansState
    data class Success(val plans: List<WorkoutPlan>) : WorkoutPlansState
    data class Error(val message: String) : WorkoutPlansState
    data object Unauthorized : WorkoutPlansState
}

sealed interface WorkoutPlanMutationState {
    data object Idle : WorkoutPlanMutationState
    data object Completing : WorkoutPlanMutationState
    data object Deactivating : WorkoutPlanMutationState
    data class Success(val message: String) : WorkoutPlanMutationState
    data class Error(val message: String) : WorkoutPlanMutationState
}

sealed interface WorkoutPlanDetailState {
    data object Loading : WorkoutPlanDetailState
    data class Success(val plan: WorkoutPlan) : WorkoutPlanDetailState
    data class Error(val message: String) : WorkoutPlanDetailState
    data object Unauthorized : WorkoutPlanDetailState
}

data class WorkoutPlanCreateState(
    val startDate: String = "",
    val endDate: String = "",
    val mode: WorkoutPlanFormMode = WorkoutPlanFormMode.CREATE,
    val creationMode: CreationMode = CreationMode.TEMPLATE,
    val planId: Long? = null,
    val loading: Boolean = false,
    val templates: List<WorkoutTemplate> = emptyList(),
    val template: WorkoutTemplate? = null,
    val templateDetail: WorkoutTemplateDetail? = null,
    val templateDetailLoading: Boolean = false,
    val assignments: Map<Long, Int> = emptyMap(),
    val drafts: Map<Long, WorkoutPlanExerciseRequest> = emptyMap(),
    val scratchDraft: WorkoutPlanDraft = WorkoutPlanDraft(),
    val error: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val createdId: Long? = null,
    val createdPlan: WorkoutPlan? = null,
)

enum class WorkoutPlanFormMode { CREATE, EDIT }

@HiltViewModel
class WorkoutPlanViewModel @Inject constructor(
    private val getPlans: GetClientWorkoutPlansUseCase,
    private val getPlan: GetWorkoutPlanDetailUseCase,
    private val createPlan: CreateWorkoutPlanUseCase,
    private val updatePlan: UpdateWorkoutPlanUseCase,
    private val completePlan: CompleteWorkoutPlanUseCase,
    private val deactivatePlan: DeactivateWorkoutPlanUseCase,
    private val getTemplates: GetWorkoutTemplatesUseCase,
    private val getTemplateDetail: GetWorkoutTemplateDetailUseCase,
    private val getExerciseCategories: GetExerciseCategoriesUseCase,
    private val getExercisesByCategory: GetExercisesByCategoryUseCase,
) : ViewModel() {
    private val _plans = MutableStateFlow<WorkoutPlansState>(WorkoutPlansState.Loading)
    val plans = _plans.asStateFlow()
    private val _detail = MutableStateFlow<WorkoutPlanDetailState>(WorkoutPlanDetailState.Loading)
    val detail = _detail.asStateFlow()
    private val _create = MutableStateFlow(WorkoutPlanCreateState())
    val createState = _create.asStateFlow()
    private val _mutation = MutableStateFlow<WorkoutPlanMutationState>(WorkoutPlanMutationState.Idle)
    val mutation = _mutation.asStateFlow()
    private val _catalog = MutableStateFlow<WorkoutPlanExerciseCatalogState>(WorkoutPlanExerciseCatalogState.Idle)
    val catalog = _catalog.asStateFlow()

    private var scope: CoroutineScope = viewModelScope
    private var nextDraftExerciseId = 1L
    private var templateSelectionGeneration = 0L
    private val createMutex = Mutex()

    internal constructor(
        getPlans: GetClientWorkoutPlansUseCase,
        getPlan: GetWorkoutPlanDetailUseCase,
        createPlan: CreateWorkoutPlanUseCase,
        updatePlan: UpdateWorkoutPlanUseCase,
        completePlan: CompleteWorkoutPlanUseCase,
        deactivatePlan: DeactivateWorkoutPlanUseCase,
        getTemplates: GetWorkoutTemplatesUseCase,
        getTemplateDetail: GetWorkoutTemplateDetailUseCase,
        getExerciseCategories: GetExerciseCategoriesUseCase,
        getExercisesByCategory: GetExercisesByCategoryUseCase,
        scope: CoroutineScope,
    ) : this(
        getPlans,
        getPlan,
        createPlan,
        updatePlan,
        completePlan,
        deactivatePlan,
        getTemplates,
        getTemplateDetail,
        getExerciseCategories,
        getExercisesByCategory,
    ) {
        this.scope = scope
    }

    fun load(clientId: Long) = scope.launch {
        _plans.value = WorkoutPlansState.Loading
        try {
            _plans.value = getPlans(clientId).let {
                if (it.isEmpty()) WorkoutPlansState.Empty else WorkoutPlansState.Success(it)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            val error = exception.toAppException()
            _plans.value = if (error is AppException.Unauthorized) {
                WorkoutPlansState.Unauthorized
            } else {
                WorkoutPlansState.Error(
                    when (error) {
                        is AppException.Network -> "No se pudo conectar con el servidor."
                        is AppException.Forbidden -> "Acceso denegado."
                        is AppException.Conflict -> "El recurso ha cambiado o existe un conflicto. Vuelve a cargar e inténtalo de nuevo."
                        else -> "No se pudieron cargar los planes."
                    },
                )
            }
        }
    }

    fun loadDetail(id: Long) = scope.launch {
        _detail.value = WorkoutPlanDetailState.Loading
        try {
            _detail.value = WorkoutPlanDetailState.Success(getPlan(id))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            val error = exception.toAppException()
            _detail.value = if (error is AppException.Unauthorized) {
                WorkoutPlanDetailState.Unauthorized
            } else {
                WorkoutPlanDetailState.Error(
                    when {
                        error is AppException.Network -> "No se pudo conectar con el servidor."
                        error is AppException.Forbidden -> "Acceso denegado."
                        error is AppException.NotFound -> "No se encontró el plan."
                        error is AppException.Conflict -> "El recurso ha cambiado o existe un conflicto. Vuelve a cargar e inténtalo de nuevo."
                        else -> "No se pudo cargar el plan."
                    },
                )
            }
        }
    }

    fun prepareCreate(clientId: Long = 0L) {
        nextDraftExerciseId = 1L
        _create.value = WorkoutPlanCreateState(
            loading = true,
            scratchDraft = WorkoutPlanDraft(clientId = clientId),
        )
        scope.launch {
            try {
                val templates = getTemplates().filter { template -> template.active }
                _create.update {
                    it.copy(
                        loading = false,
                        templates = templates,
                        creationMode = CreationMode.TEMPLATE,
                        templateDetailLoading = false,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _create.update {
                    it.copy(loading = false, error = "No se pudieron cargar las plantillas.")
                }
            }
        }
    }

    fun prepareEdit(planId: Long) {
        _create.value = WorkoutPlanCreateState(
            mode = WorkoutPlanFormMode.EDIT,
            planId = planId,
            loading = true,
        )
        scope.launch {
            try {
                val plan = getPlan(planId)
                _create.update {
                    it.copy(
                        loading = false,
                        startDate = plan.startDate,
                        endDate = plan.endDate.orEmpty(),
                        error = null,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val error = exception.toAppException()
                _create.update {
                    it.copy(
                        loading = false,
                        error = when (error) {
                            is AppException.BadRequest -> "El servidor rechazó los datos enviados."
                            is AppException.Forbidden -> "No tienes permisos para modificar este plan."
                            is AppException.NotFound -> "El plan ya no existe."
                            is AppException.Conflict -> "El plan ha cambiado o existe un conflicto. Vuelve a cargarlo antes de continuar."
                            is AppException.Network -> "No se pudo conectar con el servidor."
                            is AppException.Serialization, is AppException.InvalidResponse -> "No se pudo interpretar la respuesta del servidor."
                            is AppException.Server -> "Se ha producido un error en el servidor."
                            else -> "No se pudo cargar el plan."
                        },
                    )
                }
            }
        }
    }

    fun setStartDate(value: String) {
        _create.update {
            it.copy(
                startDate = value,
                error = null,
                scratchDraft = it.scratchDraft.copy(startDate = value),
            )
        }
    }

    fun setEndDate(value: String) {
        _create.update {
            it.copy(
                endDate = value,
                error = null,
                scratchDraft = it.scratchDraft.copy(endDate = value.ifBlank { null }),
            )
        }
    }

    fun setCreationMode(mode: CreationMode) {
        _create.update {
            it.copy(
                creationMode = mode,
                error = null,
            )
        }
    }

    fun selectFromScratch() {
        templateSelectionGeneration++
        _create.update {
            it.copy(
                creationMode = CreationMode.FROM_SCRATCH,
                template = null,
                templateDetail = null,
                templateDetailLoading = false,
                assignments = emptyMap(),
                drafts = emptyMap(),
                scratchDraft = WorkoutPlanDraft(
                    clientId = it.scratchDraft.clientId,
                    startDate = it.startDate,
                    endDate = it.endDate.ifBlank { null },
                    sourceTemplateId = null,
                ),
                error = null,
            )
        }
    }

    fun selectTemplate(template: WorkoutTemplate) {
        val generation = ++templateSelectionGeneration
        _create.update {
            it.copy(
                creationMode = CreationMode.TEMPLATE,
                template = template,
                templateDetail = null,
                templateDetailLoading = true,
                assignments = emptyMap(),
                drafts = emptyMap(),
                error = null,
            )
        }
        scope.launch {
            try {
                val detail = getTemplateDetail(template.id)
                _create.update { state ->
                    if (state.template?.id != template.id ||
                        state.creationMode != CreationMode.TEMPLATE ||
                        generation != templateSelectionGeneration
                    ) {
                        state
                    } else {
                        state.copy(
                            templateDetail = detail,
                            templateDetailLoading = false,
                            assignments = detail.exercises.associate { it.id to 1 },
                            drafts = detail.exercises.associate {
                                it.id to WorkoutPlanExerciseRequest(
                                    sourceTemplateExerciseId = it.id,
                                    exerciseId = it.exercise.id,
                                    orderIndex = it.orderIndex,
                                    sets = it.sets,
                                    repetitions = it.repetitions,
                                    restSeconds = it.restSeconds,
                                )
                            },
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _create.update { state ->
                    if (state.template?.id == template.id &&
                        state.creationMode == CreationMode.TEMPLATE &&
                        generation == templateSelectionGeneration
                    ) {
                        state.copy(templateDetailLoading = false, error = "No se pudo cargar la plantilla.")
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun assignExercise(exerciseId: Long, day: Int) {
        if (day !in 1..7) return
        _create.update { state ->
            state.copy(assignments = state.assignments + (exerciseId to day), error = null)
        }
    }

    fun updateExercise(exerciseId: Long, request: WorkoutPlanExerciseRequest) {
        _create.update { state ->
            state.copy(drafts = state.drafts + (exerciseId to request), error = null)
        }
    }

    fun addDraftDay(dayOfWeek: Int) {
        if (dayOfWeek !in 1..7) {
            _create.update { it.copy(error = "El día seleccionado no es válido.") }
            return
        }
        val state = _create.value
        if (state.scratchDraft.days.any { it.dayOfWeek == dayOfWeek }) {
            _create.update { it.copy(error = "El día ya existe en el borrador.") }
            return
        }
        _create.update {
            it.copy(
                scratchDraft = it.scratchDraft.addDay(dayOfWeek),
                error = null,
            )
        }
    }

    fun deleteDraftDay(localId: String) {
        _create.update {
            it.copy(
                scratchDraft = it.scratchDraft.removeDay(localId),
                error = null,
            )
        }
    }

    fun deleteDraftDay(dayOfWeek: Int) {
        _create.value.scratchDraft.days
            .find { it.dayOfWeek == dayOfWeek }
            ?.let { deleteDraftDay(it.localId) }
    }

    fun upsertDraftExercise(
        dayOfWeek: Int,
        request: WorkoutPlanExerciseRequest,
        exerciseName: String? = null,
        localId: Long? = null,
        dayLocalId: String? = null,
    ): Boolean {
        val state = _create.value
        val draft = if (dayLocalId == null && localId == null &&
            state.scratchDraft.days.none { it.dayOfWeek == dayOfWeek }
        ) {
            state.scratchDraft.addDay(dayOfWeek)
        } else {
            state.scratchDraft
        }
        val day = dayLocalId?.let { id -> draft.days.find { it.localId == id } }
            ?: draft.days.find { it.dayOfWeek == dayOfWeek }
        if (day == null || day.dayOfWeek !in 1..7) {
            _create.update { it.copy(error = "El día no existe en el borrador.") }
            return false
        }
        val existing = day?.exercises.orEmpty().filterNot { it.localId == localId }
        val validationError = when {
            request.exerciseId == null || request.sourceTemplateExerciseId != null ->
                "Selecciona un ejercicio."
            request.sets < 1 || request.repetitions < 1 ||
                request.restSeconds < 0 || request.orderIndex < 1 ->
                "Los datos del ejercicio no son válidos."
            existing.any { it.request.exerciseId != null && it.request.exerciseId == request.exerciseId } ->
                "El ejercicio ya existe en este día."
            existing.any { it.request.orderIndex == request.orderIndex } ->
                "El orden ya existe en este día."
            else -> null
        }
        if (validationError != null) {
            _create.update { it.copy(error = validationError) }
            return false
        }
        val resolvedId = localId ?: nextDraftExerciseId++
        _create.update {
            it.copy(
                scratchDraft = draft.upsertExercise(
                    day.localId,
                    WorkoutPlanDraftExercise(
                        localId = resolvedId,
                        request = request,
                        exerciseName = exerciseName,
                    ),
                ),
                error = null,
            )
        }
        return true
    }

    fun deleteDraftExercise(localId: Long) {
        _create.update {
            it.copy(
                scratchDraft = it.scratchDraft.removeExercise(localId),
                error = null,
            )
        }
    }

    fun loadExerciseCatalog() {
        if (_catalog.value is WorkoutPlanExerciseCatalogState.Loading) return
        _catalog.value = WorkoutPlanExerciseCatalogState.Loading
        scope.launch {
            try {
                _catalog.value = WorkoutPlanExerciseCatalogState.Categories(
                    getExerciseCategories().filter { it.active },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _catalog.value = WorkoutPlanExerciseCatalogState.Error(
                    catalogErrorMessage(exception.toAppException()),
                )
            }
        }
    }

    fun loadExercises(categoryId: Long) {
        _catalog.value = WorkoutPlanExerciseCatalogState.Loading
        scope.launch {
            try {
                _catalog.value = WorkoutPlanExerciseCatalogState.Success(
                    getExercisesByCategory(categoryId).filter { it.active },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _catalog.value = WorkoutPlanExerciseCatalogState.Error(
                    catalogErrorMessage(exception.toAppException()),
                )
            }
        }
    }

    fun retryCreate(clientId: Long, onCreated: (Long) -> Unit) {
        create(clientId, onCreated)
    }

    fun create(clientId: Long, onCreated: (Long) -> Unit) = scope.launch {
        if (!createMutex.tryLock()) return@launch
        try {
            createInternal(clientId, onCreated)
        } finally {
            createMutex.unlock()
        }
    }

    private suspend fun createInternal(clientId: Long, onCreated: (Long) -> Unit) {
        if (_create.value.saving) return
        val state = _create.value
        val scratchDraft = state.scratchDraft.copy(
            clientId = clientId,
            startDate = state.startDate,
            endDate = state.endDate.ifBlank { null },
            sourceTemplateId = null,
        )
        val days = when (state.creationMode) {
            CreationMode.TEMPLATE -> buildTemplateDays(state)
            CreationMode.FROM_SCRATCH -> scratchDraft.toRequests()
        }
        val request = CreateWorkoutPlanRequest(
            sourceTemplateId = state.template?.id.takeIf { state.creationMode == CreationMode.TEMPLATE },
            startDate = state.startDate,
            endDate = state.endDate.ifBlank { null },
            days = days,
        )
        if (state.creationMode == CreationMode.FROM_SCRATCH) {
            _create.update { it.copy(scratchDraft = scratchDraft) }
        }

        val templateStateValid = state.creationMode != CreationMode.TEMPLATE ||
            (!state.templateDetailLoading &&
                state.template != null &&
                state.templateDetail?.template?.id == state.template.id &&
                state.assignments.keys.all { id ->
                    state.templateDetail.exercises.any { it.id == id }
                } &&
                state.drafts.keys.all { id ->
                    state.templateDetail.exercises.any { it.id == id }
                })
        if (!templateStateValid) {
            _create.update { it.copy(error = "Espera a que la plantilla termine de cargarse.") }
            return
        }
        val draftValidation = if (state.creationMode == CreationMode.FROM_SCRATCH) {
            scratchDraft.validationError()
        } else {
            null
        }
        val validationError = draftValidation ?: request.validationError()
        if (validationError != null) {
            _create.update { it.copy(error = validationError) }
            return
        }

        _create.update { it.copy(saving = true, error = null, saved = false) }
        try {
            val created = createPlan(clientId, request)
            _detail.value = WorkoutPlanDetailState.Success(created)
            _create.update {
                it.copy(
                    saving = false,
                    createdId = created.id,
                    createdPlan = created,
                    saved = true,
                )
            }
            onCreated(created.id)
        } catch (exception: CancellationException) {
            _create.update { it.copy(saving = false) }
            throw exception
        } catch (exception: Exception) {
            val error = exception.toAppException()
            _create.update {
                it.copy(saving = false, error = errorMessage(error, editing = false))
            }
        }
    }

    fun update(onSaved: (Long) -> Unit) = scope.launch {
        if (_create.value.saving) return@launch
        val state = _create.value
        val planId = state.planId ?: return@launch
        val request = UpdateWorkoutPlanRequest(
            startDate = state.startDate,
            endDate = state.endDate.ifBlank { null },
        )
        validateUpdate(request)?.let { message ->
            _create.update { it.copy(error = message) }
            return@launch
        }

        _create.update { it.copy(saving = true, error = null, saved = false) }
        try {
            val updated = updatePlan(planId, request)
            _detail.value = WorkoutPlanDetailState.Success(updated)
            _create.update {
                it.copy(
                    saving = false,
                    saved = true,
                    createdId = updated.id,
                    createdPlan = updated,
                )
            }
            onSaved(updated.id)
        } catch (exception: CancellationException) {
            _create.update { it.copy(saving = false) }
            throw exception
        } catch (exception: Exception) {
            _create.update {
                it.copy(saving = false, error = errorMessage(exception.toAppException(), editing = true))
            }
        }
    }

    fun loadDetailIfNeeded(id: Long) {
        if ((_detail.value as? WorkoutPlanDetailState.Success)?.plan?.id != id) loadDetail(id)
    }

    fun setDetailPlan(plan: WorkoutPlan) {
        _detail.value = WorkoutPlanDetailState.Success(plan)
    }

    fun clearMutation() {
        _mutation.value = WorkoutPlanMutationState.Idle
    }

    fun complete(id: Long) = scope.launch {
        if (_mutation.value is WorkoutPlanMutationState.Completing ||
            _mutation.value is WorkoutPlanMutationState.Deactivating
        ) return@launch

        val current = (_detail.value as? WorkoutPlanDetailState.Success)?.plan
        if (current?.id != id || current.status != "ACTIVE") return@launch

        _mutation.value = WorkoutPlanMutationState.Completing
        try {
            completePlan(id)
            _detail.value = WorkoutPlanDetailState.Success(current.copy(status = "COMPLETED"))
            _mutation.value = WorkoutPlanMutationState.Success("Plan completado correctamente.")
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            _mutation.value = WorkoutPlanMutationState.Error(mutationError(exception.toAppException()))
        }
    }

    fun deactivate(id: Long) = scope.launch {
        if (_mutation.value is WorkoutPlanMutationState.Completing ||
            _mutation.value is WorkoutPlanMutationState.Deactivating
        ) return@launch

        val current = (_detail.value as? WorkoutPlanDetailState.Success)?.plan
        if (current?.id != id || current.status != "ACTIVE") return@launch

        _mutation.value = WorkoutPlanMutationState.Deactivating
        try {
            deactivatePlan(id)
            _detail.value = WorkoutPlanDetailState.Success(current.copy(status = "INACTIVE"))
            _mutation.value = WorkoutPlanMutationState.Success("Plan desactivado correctamente.")
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            _mutation.value = WorkoutPlanMutationState.Error(mutationError(exception.toAppException()))
        }
    }

    private fun buildTemplateDays(state: WorkoutPlanCreateState): List<WorkoutPlanDayRequest> =
        state.assignments.values.distinct().sorted().map { day ->
            WorkoutPlanDayRequest(
                dayOfWeek = day,
                exercises = state.assignments
                    .filterValues { assignedDay -> assignedDay == day }
                    .keys
                    .mapNotNull { state.drafts[it] }
                    .sortedBy { it.orderIndex },
            )
        }

    private fun validateUpdate(request: UpdateWorkoutPlanRequest): String? {
        val start = request.startDate
        val end = request.endDate
        if (start.isNullOrBlank()) return "La fecha de inicio es obligatoria."
        if (!isIsoDate(start)) return "La fecha de inicio no es válida."
        if (end != null && !isIsoDate(end)) return "La fecha de fin no es válida."
        if (end != null && end < start) return "La fecha de fin debe ser posterior o igual."
        return null
    }

    private fun isIsoDate(value: String): Boolean =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
            .let { format ->
                ParsePosition(0).let { position ->
                    format.parse(value, position) != null && position.index == value.length
                }
            }

    private fun errorMessage(error: AppException, editing: Boolean): String = when (error) {
        is AppException.BadRequest -> "El servidor rechazó los datos enviados. Revisa la información e inténtalo de nuevo."
        is AppException.Forbidden -> if (editing) "No tienes permisos para modificar este plan." else "Acceso denegado."
        is AppException.NotFound -> if (editing) "El plan ya no existe." else "No se encontró el plan."
        is AppException.Conflict -> "El plan ha cambiado o existe un conflicto. Vuelve a cargarlo antes de continuar."
        is AppException.Server -> "Se ha producido un error en el servidor."
        is AppException.Network -> "No se pudo conectar con el servidor."
        is AppException.Serialization, is AppException.InvalidResponse -> "No se pudo interpretar la respuesta del servidor."
        else -> "No se pudo ${if (editing) "guardar" else "crear"} el plan."
    }

    private fun mutationError(error: AppException): String = when (error) {
        is AppException.BadRequest -> "El plan no puede cambiar a este estado."
        is AppException.Forbidden -> "No tienes permisos para modificar este plan."
        is AppException.NotFound -> "El plan ya no existe."
        is AppException.Conflict -> "El plan ha cambiado o existe un conflicto. Vuelve a cargarlo antes de continuar."
        is AppException.Server -> "El servidor no está disponible. Inténtalo de nuevo."
        is AppException.Network -> "No se ha podido conectar con el servidor."
        is AppException.Serialization, is AppException.InvalidResponse -> "La respuesta del servidor no es válida."
        else -> "Ha ocurrido un error inesperado."
    }

    private fun catalogErrorMessage(error: AppException): String = when (error) {
        is AppException.Network -> "No se ha podido conectar con el servidor."
        is AppException.Serialization, is AppException.InvalidResponse ->
            "La respuesta del servidor no es válida."
        is AppException.Forbidden -> "No tienes permisos para consultar los ejercicios."
        else -> "No se pudieron cargar los ejercicios."
    }
}
