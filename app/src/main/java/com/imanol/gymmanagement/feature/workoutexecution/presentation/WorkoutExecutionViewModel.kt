package com.imanol.gymmanagement.feature.workoutexecution.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.workoutexecution.data.AndroidMonotonicClock
import com.imanol.gymmanagement.feature.workoutexecution.domain.MonotonicClock
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionSnapshot
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionState
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionUseCase
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionStatus
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutRestTimer
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutRestTimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkoutExecutionViewModel(
    val snapshot: WorkoutExecutionSnapshot,
    private val clock: MonotonicClock = AndroidMonotonicClock(),
    tickerEnabled: Boolean = true,
) : ViewModel() {
    private val execution = WorkoutExecutionUseCase(snapshot)
    private val restTimer = WorkoutRestTimer(
        clock = clock,
        scope = viewModelScope,
        tickerEnabled = tickerEnabled,
        onFinished = {},
    )
    private val _uiState = MutableStateFlow(execution.state)
    val uiState: StateFlow<WorkoutExecutionState> = _uiState.asStateFlow()
    val restTimerState: StateFlow<WorkoutRestTimerState> = restTimer.state

    fun start() = update { execution.start() }

    fun completeCurrentSet() {
        val previousStatus = execution.state.status
        update { execution.completeCurrentSet() }
        if (previousStatus != WorkoutExecutionStatus.Resting &&
            execution.state.status == WorkoutExecutionStatus.Resting
        ) {
            restTimer.start(execution.currentRestSeconds)
        }
    }

    fun finishRest() {
        restTimer.stop()
        update { execution.finishRest() }
    }

    fun cancel() {
        restTimer.stop()
        update { execution.cancel() }
    }

    fun finish() {
        restTimer.stop()
        update { execution.finish() }
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

}

class WorkoutExecutionViewModelFactory(
    private val snapshot: WorkoutExecutionSnapshot,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        check(modelClass.isAssignableFrom(WorkoutExecutionViewModel::class.java))
        return WorkoutExecutionViewModel(snapshot) as T
    }
}
