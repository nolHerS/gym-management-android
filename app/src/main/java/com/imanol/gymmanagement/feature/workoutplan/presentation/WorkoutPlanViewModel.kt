package com.imanol.gymmanagement.feature.workoutplan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.workout.domain.*
import com.imanol.gymmanagement.feature.workoutplan.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.core.network.toAppException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

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
    val startDate: String = "", val endDate: String = "",
    val mode: WorkoutPlanFormMode = WorkoutPlanFormMode.CREATE,
    val planId: Long? = null,
    val loading: Boolean = false,
    val templates: List<WorkoutTemplate> = emptyList(),
    val template: WorkoutTemplate? = null,
    val templateDetail: WorkoutTemplateDetail? = null,
    val assignments: Map<Long, Int> = emptyMap(),
    val drafts: Map<Long, WorkoutPlanExerciseRequest> = emptyMap(),
    val error: String? = null, val saving: Boolean = false, val saved: Boolean = false,
    val createdId: Long? = null,
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
) : ViewModel() {
    private val _plans = MutableStateFlow<WorkoutPlansState>(WorkoutPlansState.Loading)
    val plans = _plans.asStateFlow()
    private val _detail = MutableStateFlow<WorkoutPlanDetailState>(WorkoutPlanDetailState.Loading)
    val detail = _detail.asStateFlow()
    private val _create = MutableStateFlow(WorkoutPlanCreateState())
    val createState = _create.asStateFlow()
    private val _mutation = MutableStateFlow<WorkoutPlanMutationState>(WorkoutPlanMutationState.Idle)
    val mutation = _mutation.asStateFlow()
    private var scope: CoroutineScope = viewModelScope

    internal constructor(
        getPlans: GetClientWorkoutPlansUseCase, getPlan: GetWorkoutPlanDetailUseCase,
        createPlan: CreateWorkoutPlanUseCase, updatePlan: UpdateWorkoutPlanUseCase,
        completePlan: CompleteWorkoutPlanUseCase, deactivatePlan: DeactivateWorkoutPlanUseCase,
        getTemplates: GetWorkoutTemplatesUseCase,
        getTemplateDetail: GetWorkoutTemplateDetailUseCase, scope: CoroutineScope,
    ) : this(
        getPlans,
        getPlan,
        createPlan,
        updatePlan,
        completePlan,
        deactivatePlan,
        getTemplates,
        getTemplateDetail,
    ) { this.scope = scope }

    fun load(clientId: Long) = scope.launch {
        _plans.value = WorkoutPlansState.Loading
        try { _plans.value = getPlans(clientId).let { if (it.isEmpty()) WorkoutPlansState.Empty else WorkoutPlansState.Success(it) } }
        catch (exception: CancellationException) { throw exception }
        catch (exception: Exception) {
            val e = exception.toAppException()
            _plans.value = if (e is AppException.Unauthorized) WorkoutPlansState.Unauthorized
            else WorkoutPlansState.Error(
                when {
                    e is AppException.Network -> "No se pudo conectar con el servidor."
                    e is AppException.Forbidden -> "Acceso denegado."
                    e is AppException.Conflict -> "El recurso ha cambiado o existe un conflicto. Vuelve a cargar e inténtalo de nuevo."
                    else -> "No se pudieron cargar los planes."
                },
            )
        }
    }
    fun loadDetail(id: Long) = scope.launch {
        _detail.value = WorkoutPlanDetailState.Loading
        try { _detail.value = WorkoutPlanDetailState.Success(getPlan(id)) }
        catch (exception: CancellationException) { throw exception }
        catch (exception: Exception) {
            val e = exception.toAppException()
            _detail.value = if (e is AppException.Unauthorized) {
                WorkoutPlanDetailState.Unauthorized
            } else {
                WorkoutPlanDetailState.Error(
                    when {
                        e is AppException.Network -> "No se pudo conectar con el servidor."
                        e is AppException.Forbidden -> "Acceso denegado."
                        e is AppException.NotFound -> "No se encontró el plan."
                        e is AppException.Conflict -> "El recurso ha cambiado o existe un conflicto. Vuelve a cargar e inténtalo de nuevo."
                        else -> "No se pudo cargar el plan."
                    },
                )
            }
        }
    }
    fun prepareCreate() {
        _create.value = WorkoutPlanCreateState(loading = true)
        scope.launch {
            try {
                val templates = getTemplates()
                _create.update { it.copy(loading = false, templates = templates.filter { template -> template.active }) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _create.update { it.copy(loading = false, error = "No se pudieron cargar las plantillas.") }
            }
        }
    }

    fun prepareEdit(planId: Long) {
        _create.value = WorkoutPlanCreateState(mode = WorkoutPlanFormMode.EDIT, planId = planId, loading = true)
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
    fun setStartDate(v: String) { _create.update { it.copy(startDate = v, error = null) } }
    fun setEndDate(v: String) { _create.update { it.copy(endDate = v, error = null) } }
    fun selectFromScratch() { _create.update { it.copy(template = null, templateDetail = null, assignments = emptyMap(), drafts = emptyMap(), error = null) } }
    fun selectTemplate(template: WorkoutTemplate) {
        _create.update { it.copy(template = template, templateDetail = null, error = null) }
        scope.launch {
            runCatching { getTemplateDetail(template.id) }
                .onSuccess { detail ->
                    _create.update { state ->
                        state.copy(
                            templateDetail = detail,
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
                .onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    _create.update { it.copy(error = "No se pudo cargar la plantilla.") }
                }
        }
    }
    fun assignExercise(exerciseId: Long, day: Int) {
        if (day in 1..7) _create.update { it.copy(assignments = it.assignments + (exerciseId to day)) }
    }
    fun updateExercise(exerciseId: Long, request: WorkoutPlanExerciseRequest) {
        _create.update { it.copy(drafts = it.drafts + (exerciseId to request), error = null) }
    }
    fun create(clientId: Long, onCreated: (Long) -> Unit) = scope.launch {
        if (_create.value.saving) return@launch
        val s = _create.value; val detail = s.templateDetail
        val days = s.assignments.values.distinct().sorted().map { day ->
            WorkoutPlanDayRequest(
                day,
                s.assignments.filterValues { it == day }.keys.mapNotNull { s.drafts[it] },
            )
        }
        val request = CreateWorkoutPlanRequest(s.template?.id, s.startDate, s.endDate.ifBlank { null }, days)
        request.validationError()?.let { message -> _create.update { it.copy(error = message) }; return@launch }
        _create.update { it.copy(saving = true, error = null) }
        try { val id = createPlan(clientId, request).id; _create.update { it.copy(saving = false, createdId = id) }; onCreated(id) }
        catch (exception: CancellationException) { throw exception }
        catch (exception: Exception) {
            val error = exception.toAppException()
            _create.update { it.copy(saving = false, error = errorMessage(error, editing = false)) }
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
            _create.update { it.copy(saving = false, saved = true) }
            onSaved(updated.id)
        } catch (exception: CancellationException) {
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

    private fun validateUpdate(request: UpdateWorkoutPlanRequest): String? {
        val start = request.startDate
        val end = request.endDate
        if (start.isNullOrBlank()) return "La fecha de inicio es obligatoria."
        if (!isIsoDate(start)) {
            return "La fecha de inicio no es válida."
        }
        if (end != null && !isIsoDate(end)) {
            return "La fecha de fin no es válida."
        }
        if (end != null && end < start) return "La fecha de fin debe ser posterior o igual."
        return null
    }

    private fun isIsoDate(value: String): Boolean =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }.let { format ->
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
}
