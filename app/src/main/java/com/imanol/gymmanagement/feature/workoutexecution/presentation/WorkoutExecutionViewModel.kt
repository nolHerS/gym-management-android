package com.imanol.gymmanagement.feature.workoutexecution.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.core.network.toAppException
import com.imanol.gymmanagement.feature.workoutexecution.data.AndroidMonotonicClock
import com.imanol.gymmanagement.feature.workoutexecution.domain.MonotonicClock
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionSetPosition
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionSnapshot
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionState
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionStatus
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionUseCase
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutRestTimer
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutRestTimerState
import com.imanol.gymmanagement.feature.workoutsession.domain.AddWorkoutSessionSetUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.CancelWorkoutSessionUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.FinishWorkoutSessionUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.UpdateWorkoutSessionExerciseUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSession
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionExerciseUpdate
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionSetInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WorkoutExecutionSyncState {
    data object Idle : WorkoutExecutionSyncState
    data object Syncing : WorkoutExecutionSyncState
    data object Synced : WorkoutExecutionSyncState
    data class Error(val message: String) : WorkoutExecutionSyncState
}

class WorkoutExecutionViewModel(
    val snapshot: WorkoutExecutionSnapshot,
    private val clock: MonotonicClock = AndroidMonotonicClock(),
    tickerEnabled: Boolean = true,
    private val session: WorkoutSession? = null,
    private val addSet: AddWorkoutSessionSetUseCase? = null,
    private val updateExercise: UpdateWorkoutSessionExerciseUseCase? = null,
    private val finishSession: FinishWorkoutSessionUseCase? = null,
    private val cancelSession: CancelWorkoutSessionUseCase? = null,
    syncScope: CoroutineScope? = null,
) : ViewModel() {
    val sessionId: Long?
        get() = session?.id

    private val execution = WorkoutExecutionUseCase(snapshot)
    private val restTimer = WorkoutRestTimer(
        clock = clock,
        scope = viewModelScope,
        tickerEnabled = tickerEnabled,
        onFinished = {},
    )
    private val operationScope = syncScope ?: viewModelScope
    private val _uiState = MutableStateFlow(execution.state)
    val uiState: StateFlow<WorkoutExecutionState> = _uiState.asStateFlow()
    private val _syncState = MutableStateFlow<WorkoutExecutionSyncState>(
        WorkoutExecutionSyncState.Idle,
    )
    val syncState: StateFlow<WorkoutExecutionSyncState> = _syncState.asStateFlow()
    val restTimerState: StateFlow<WorkoutRestTimerState> = restTimer.state

    private var syncJob: Job? = null
    private var operationInFlight = false
    private val registeredSets = mutableSetOf<WorkoutExecutionSetPosition>()
    private val updatedExercises = mutableSetOf<Int>()
    private var finishSynced = false
    private var cancelSynced = false
    private var retryOperation: (suspend () -> Unit)? = null

    fun start() = update { execution.start() }

    fun completeCurrentSet() {
        if (operationInFlight ||
            _syncState.value is WorkoutExecutionSyncState.Error ||
            execution.state.status != WorkoutExecutionStatus.Running
        ) return
        val position = WorkoutExecutionSetPosition(
            execution.state.currentExerciseIndex,
            execution.state.currentSetNumber,
        )
        if (position in registeredSets) return
        val previousStatus = execution.state.status
        update { execution.completeCurrentSet() }
        if (previousStatus != WorkoutExecutionStatus.Resting &&
            execution.state.status == WorkoutExecutionStatus.Resting
        ) {
            restTimer.start(execution.currentRestSeconds)
        }
        if (session != null) {
            syncJob = launchSync { syncCompletedSet(position) }
        }
    }

    fun finishRest() {
        if (_syncState.value is WorkoutExecutionSyncState.Error) return
        restTimer.stop()
        update { execution.finishRest() }
    }

    fun cancel() {
        if (operationInFlight) return
        restTimer.stop()
        update { execution.cancel() }
        if (session != null && !cancelSynced) {
            syncJob = launchSync {
                cancelSession?.invoke(session.id)
                cancelSynced = true
            }
        }
    }

    fun finish() {
        if (operationInFlight) return
        restTimer.stop()
        update { execution.finish() }
        if (session != null && !finishSynced) {
            syncJob = launchSync { syncFinishedSession() }
        }
    }

    fun retrySynchronization() {
        if (operationInFlight) return
        retryOperation?.let { syncJob = launchSync(it) }
    }

    fun refreshRestTimer() {
        restTimer.refresh()
    }

    override fun onCleared() {
        restTimer.stop()
        super.onCleared()
    }

    private fun update(operation: () -> WorkoutExecutionState) {
        _uiState.value = operation()
    }

    private fun launchSync(operation: suspend () -> Unit): Job {
        retryOperation = operation
        operationInFlight = true
        return operationScope.launch {
            _syncState.value = WorkoutExecutionSyncState.Syncing
            try {
                operation()
                retryOperation = null
                _syncState.value = WorkoutExecutionSyncState.Synced
            } catch (exception: CancellationException) {
                throw exception
            } catch (throwable: Throwable) {
                _syncState.value = WorkoutExecutionSyncState.Error(
                    syncErrorMessage(throwable.toAppException()),
                )
            } finally {
                operationInFlight = false
            }
        }
    }

    private suspend fun syncCompletedSet(position: WorkoutExecutionSetPosition) {
        val currentSession = requireNotNull(session)
        val snapshotExercise = snapshot.exercises[position.exerciseIndex]
        val sessionExerciseId = currentSession.exercises
            .firstOrNull { it.orderIndex == snapshotExercise.orderIndex }
            ?.id
            ?: error("Workout session exercise mapping is missing")

        if (position !in registeredSets) {
            addSet?.invoke(
                currentSession.id,
                WorkoutSessionSetInput(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = position.setNumber,
                ),
            )
            registeredSets += position
        }

        if (position.setNumber == snapshotExercise.sets.size &&
            position.exerciseIndex !in updatedExercises
        ) {
            updateExercise?.invoke(
                currentSession.id,
                sessionExerciseId,
                WorkoutSessionExerciseUpdate(completed = true),
            )
            updatedExercises += position.exerciseIndex
        }

        if (execution.state.status == WorkoutExecutionStatus.Finished) {
            syncFinishedSession()
        }
    }

    private suspend fun syncFinishedSession() {
        if (!finishSynced) {
            finishSession?.invoke(requireNotNull(session).id)
            finishSynced = true
        }
    }
}

private fun syncErrorMessage(exception: AppException): String = when (exception) {
    is AppException.Unauthorized -> "La sesión no es válida."
    is AppException.Forbidden -> "Acceso denegado."
    is AppException.NotFound -> "La sesión de entrenamiento ya no existe."
    is AppException.Conflict -> "La sesión ha cambiado. Inténtalo de nuevo."
    is AppException.Network -> "No se pudo conectar con el servidor. Inténtalo de nuevo."
    else -> "No se pudo sincronizar el entrenamiento. Inténtalo de nuevo."
}

class WorkoutExecutionViewModelFactory(
    private val snapshot: WorkoutExecutionSnapshot,
    private val session: WorkoutSession,
    private val addSet: AddWorkoutSessionSetUseCase,
    private val updateExercise: UpdateWorkoutSessionExerciseUseCase,
    private val finishSession: FinishWorkoutSessionUseCase,
    private val cancelSession: CancelWorkoutSessionUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        check(modelClass.isAssignableFrom(WorkoutExecutionViewModel::class.java))
        return WorkoutExecutionViewModel(
            snapshot = snapshot,
            session = session,
            addSet = addSet,
            updateExercise = updateExercise,
            finishSession = finishSession,
            cancelSession = cancelSession,
        ).also { it.start() } as T
    }
}
