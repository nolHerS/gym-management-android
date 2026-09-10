package com.imanol.gymmanagement.feature.workoutplan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.workout.domain.*
import com.imanol.gymmanagement.feature.workoutplan.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface WorkoutPlansState {
    data object Loading : WorkoutPlansState
    data object Empty : WorkoutPlansState
    data class Success(val plans: List<WorkoutPlan>) : WorkoutPlansState
    data class Error(val message: String) : WorkoutPlansState
    data object Unauthorized : WorkoutPlansState
}
sealed interface WorkoutPlanDetailState {
    data object Loading : WorkoutPlanDetailState
    data class Success(val plan: WorkoutPlan) : WorkoutPlanDetailState
    data class Error(val message: String) : WorkoutPlanDetailState
    data object Unauthorized : WorkoutPlanDetailState
}
data class WorkoutPlanCreateState(
    val startDate: String = "", val endDate: String = "",
    val templates: List<WorkoutTemplate> = emptyList(),
    val template: WorkoutTemplate? = null,
    val templateDetail: WorkoutTemplateDetail? = null,
    val assignments: Map<Long, Int> = emptyMap(),
    val drafts: Map<Long, WorkoutPlanExerciseRequest> = emptyMap(),
    val error: String? = null, val saving: Boolean = false, val createdId: Long? = null,
)

@HiltViewModel
class WorkoutPlanViewModel @Inject constructor(
    private val getPlans: GetClientWorkoutPlansUseCase,
    private val getPlan: GetWorkoutPlanDetailUseCase,
    private val createPlan: CreateWorkoutPlanUseCase,
    private val getTemplates: GetWorkoutTemplatesUseCase,
    private val getTemplateDetail: GetWorkoutTemplateDetailUseCase,
) : ViewModel() {
    private val _plans = MutableStateFlow<WorkoutPlansState>(WorkoutPlansState.Loading)
    val plans = _plans.asStateFlow()
    private val _detail = MutableStateFlow<WorkoutPlanDetailState>(WorkoutPlanDetailState.Loading)
    val detail = _detail.asStateFlow()
    private val _create = MutableStateFlow(WorkoutPlanCreateState())
    val createState = _create.asStateFlow()
    private var scope: CoroutineScope = viewModelScope

    internal constructor(
        getPlans: GetClientWorkoutPlansUseCase, getPlan: GetWorkoutPlanDetailUseCase,
        createPlan: CreateWorkoutPlanUseCase, getTemplates: GetWorkoutTemplatesUseCase,
        getTemplateDetail: GetWorkoutTemplateDetailUseCase, scope: CoroutineScope,
    ) : this(getPlans, getPlan, createPlan, getTemplates, getTemplateDetail) { this.scope = scope }

    fun load(clientId: Long) = scope.launch {
        _plans.value = WorkoutPlansState.Loading
        try { _plans.value = getPlans(clientId).let { if (it.isEmpty()) WorkoutPlansState.Empty else WorkoutPlansState.Success(it) } }
        catch (e: HttpException) { _plans.value = if (e.code() == 401 || e.code() == 403) WorkoutPlansState.Unauthorized else WorkoutPlansState.Error("No se pudieron cargar los planes.") }
        catch (_: IOException) { _plans.value = WorkoutPlansState.Error("No se pudo conectar con el servidor.") }
    }
    fun loadDetail(id: Long) = scope.launch {
        _detail.value = WorkoutPlanDetailState.Loading
        try { _detail.value = WorkoutPlanDetailState.Success(getPlan(id)) }
        catch (e: HttpException) {
            _detail.value = if (e.code() == 401 || e.code() == 403) {
                WorkoutPlanDetailState.Unauthorized
            } else {
                WorkoutPlanDetailState.Error("No se pudo cargar el plan.")
            }
        }
        catch (_: Exception) { _detail.value = WorkoutPlanDetailState.Error("No se pudo cargar el plan.") }
    }
    fun prepareCreate() {
        _create.value = WorkoutPlanCreateState()
        scope.launch {
            runCatching { getTemplates() }
                .onSuccess { templates -> _create.update { it.copy(templates = templates.filter { template -> template.active }) } }
                .onFailure { _create.update { it.copy(error = "No se pudieron cargar las plantillas.") } }
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
                .onFailure { _create.update { it.copy(error = "No se pudo cargar la plantilla.") } }
        }
    }
    fun assignExercise(exerciseId: Long, day: Int) {
        if (day in 1..7) _create.update { it.copy(assignments = it.assignments + (exerciseId to day)) }
    }
    fun updateExercise(exerciseId: Long, request: WorkoutPlanExerciseRequest) {
        _create.update { it.copy(drafts = it.drafts + (exerciseId to request), error = null) }
    }
    fun create(clientId: Long, onCreated: (Long) -> Unit) = scope.launch {
        val s = _create.value; val detail = s.templateDetail
        val days = s.assignments.values.distinct().sorted().map { day ->
            WorkoutPlanDayRequest(
                day,
                s.assignments.filterValues { it == day }.keys.mapNotNull { s.drafts[it] },
            )
        }
        val request = CreateWorkoutPlanRequest(s.template?.id, s.startDate, s.endDate.ifBlank { null }, days)
        request.validationError()?.let { message -> _create.update { it.copy(error = message) }; return@launch }
        _create.update { it.copy(saving = true) }
        try { val id = createPlan(clientId, request).id; _create.update { it.copy(saving = false, createdId = id) }; onCreated(id) }
        catch (_: Exception) { _create.update { it.copy(saving = false, error = "No se pudo crear el plan.") } }
    }
}
