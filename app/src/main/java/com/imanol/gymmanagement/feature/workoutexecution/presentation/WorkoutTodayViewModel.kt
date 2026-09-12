package com.imanol.gymmanagement.feature.workoutexecution.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.core.network.toAppException
import com.imanol.gymmanagement.feature.workoutexecution.domain.CreateWorkoutExecutionSnapshotUseCase
import com.imanol.gymmanagement.feature.workoutexecution.domain.InvalidWorkoutExecutionException
import com.imanol.gymmanagement.feature.workoutexecution.domain.SelectWorkoutPlanForExecutionUseCase
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionDateProvider
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionPlanSelection
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionSnapshot
import com.imanol.gymmanagement.feature.workoutexecution.domain.isoDayOfWeek
import com.imanol.gymmanagement.feature.workoutexecution.domain.mondayOfIsoWeek
import com.imanol.gymmanagement.feature.workoutplan.domain.GetMyWorkoutWeekUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
import com.imanol.gymmanagement.feature.workoutsession.domain.CreateWorkoutSessionUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WorkoutTodayUiState {
    data object Loading : WorkoutTodayUiState

    data class Success(
        val date: String,
        val dayOfWeek: Int,
        val plan: WorkoutPlan,
    ) : WorkoutTodayUiState

    data object Empty : WorkoutTodayUiState

    data class Ambiguous(val plans: List<WorkoutPlan>) : WorkoutTodayUiState

    data class Error(val message: String) : WorkoutTodayUiState

    data object Unauthorized : WorkoutTodayUiState
}

@HiltViewModel
class WorkoutTodayViewModel @Inject constructor(
    private val getMyWorkoutWeek: GetMyWorkoutWeekUseCase,
    private val selectPlan: SelectWorkoutPlanForExecutionUseCase,
    private val createSnapshot: CreateWorkoutExecutionSnapshotUseCase,
    private val createWorkoutSession: CreateWorkoutSessionUseCase,
    private val dateProvider: WorkoutExecutionDateProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow<WorkoutTodayUiState>(WorkoutTodayUiState.Loading)
    val uiState: StateFlow<WorkoutTodayUiState> = _uiState.asStateFlow()
    private val _startState = MutableStateFlow(WorkoutTodayStartState())
    val startState: StateFlow<WorkoutTodayStartState> = _startState.asStateFlow()

    private var providedScope: CoroutineScope? = null
    private var loadJob: Job? = null
    private var loadGeneration = 0L
    private var loadedDate: String? = null
    private var pendingSnapshot: WorkoutExecutionSnapshot? = null
    private var startJob: Job? = null
    private var createdSession: WorkoutSession? = null

    internal constructor(
        getMyWorkoutWeek: GetMyWorkoutWeekUseCase,
        selectPlan: SelectWorkoutPlanForExecutionUseCase,
        createSnapshot: CreateWorkoutExecutionSnapshotUseCase,
        createWorkoutSession: CreateWorkoutSessionUseCase,
        dateProvider: WorkoutExecutionDateProvider,
        scope: CoroutineScope,
    ) : this(getMyWorkoutWeek, selectPlan, createSnapshot, createWorkoutSession, dateProvider) {
        providedScope = scope
    }

    fun loadToday() {
        val today = dateProvider.todayIsoDate()
        if (_uiState.value is WorkoutTodayUiState.Loading &&
            loadedDate == today &&
            loadJob?.isActive == true
        ) {
            return
        }
        loadedDate = today
        val generation = ++loadGeneration
        loadJob?.cancel()
        _uiState.value = WorkoutTodayUiState.Loading
        loadJob = (providedScope ?: viewModelScope).launch {
            try {
                val dayOfWeek = isoDayOfWeek(today)
                val plans = getMyWorkoutWeek(mondayOfIsoWeek(today))
                if (generation == loadGeneration) {
                    _uiState.value = when (val selection = selectPlan(plans, dayOfWeek)) {
                        WorkoutExecutionPlanSelection.NoCandidate -> WorkoutTodayUiState.Empty
                        is WorkoutExecutionPlanSelection.Ambiguous ->
                            WorkoutTodayUiState.Ambiguous(selection.plans)
                        is WorkoutExecutionPlanSelection.Selected -> {
                            createSnapshot.validate(selection.plan, dayOfWeek)
                            WorkoutTodayUiState.Success(today, dayOfWeek, selection.plan)
                        }
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: InvalidWorkoutExecutionException) {
                if (generation == loadGeneration) {
                    _uiState.value = WorkoutTodayUiState.Error(
                        "El entrenamiento de hoy no es válido.",
                    )
                }
            } catch (throwable: Exception) {
                if (generation != loadGeneration) return@launch
                val exception = throwable.toAppException()
                _uiState.value = when (exception) {
                    is AppException.Unauthorized -> WorkoutTodayUiState.Unauthorized
                    is AppException.Network -> WorkoutTodayUiState.Error(
                        "No se pudo conectar con el servidor. Inténtalo de nuevo.",
                    )
                    else -> WorkoutTodayUiState.Error(
                        if (exception is AppException.Forbidden) "Acceso denegado."
                        else "No se pudo cargar el entrenamiento de hoy. Inténtalo de nuevo.",
                    )
                }
            }
        }
    }

    fun retry() {
        loadedDate = null
        loadToday()
    }

    fun createSnapshot(onCreated: (WorkoutExecutionSnapshot) -> Unit) {
        val state = _uiState.value as? WorkoutTodayUiState.Success ?: return
        try {
            onCreated(createSnapshot(state.plan, state.dayOfWeek))
        } catch (_: InvalidWorkoutExecutionException) {
            _uiState.value = WorkoutTodayUiState.Error(
                "El entrenamiento ha cambiado y ya no es válido. Inténtalo de nuevo.",
            )
        }
    }

    fun startWorkout(onCreated: (WorkoutExecutionSnapshot, WorkoutSession) -> Unit) {
        if (_startState.value.isStarting || startJob?.isActive == true || createdSession != null) return
        val state = _uiState.value as? WorkoutTodayUiState.Success ?: return
        _startState.value = WorkoutTodayStartState(isStarting = true)
        startJob = (providedScope ?: viewModelScope).launch {
            try {
                val snapshot = createSnapshot(state.plan, state.dayOfWeek)
                pendingSnapshot = snapshot
                val session = createWorkoutSession(snapshot.planId)
                createdSession = session
                _startState.value = WorkoutTodayStartState()
                onCreated(snapshot, session)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: InvalidWorkoutExecutionException) {
                _startState.value = WorkoutTodayStartState(
                    errorMessage = "El entrenamiento ha cambiado y ya no es válido. Inténtalo de nuevo.",
                )
            } catch (throwable: Throwable) {
                val exception = throwable.toAppException()
                _startState.value = WorkoutTodayStartState(
                    errorMessage = startErrorMessage(exception),
                )
            }
        }
    }

    fun retryStart(onCreated: (WorkoutExecutionSnapshot, WorkoutSession) -> Unit) {
        startWorkout(onCreated)
    }
}

data class WorkoutTodayStartState(
    val isStarting: Boolean = false,
    val errorMessage: String? = null,
)

private fun startErrorMessage(exception: AppException): String = when (exception) {
    is AppException.Unauthorized -> "La sesión no es válida."
    is AppException.Forbidden -> "Acceso denegado."
    is AppException.Network -> "No se pudo conectar con el servidor. Inténtalo de nuevo."
    is AppException.Conflict -> "Ya existe una sesión activa para este entrenamiento."
    else -> "No se pudo comenzar el entrenamiento. Inténtalo de nuevo."
}
