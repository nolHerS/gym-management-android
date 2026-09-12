package com.imanol.gymmanagement.feature.workoutexecution.domain

import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface MonotonicClock {
    fun elapsedRealtime(): Long
}

enum class WorkoutRestTimerStatus {
    Idle,
    Running,
    Finished,
}

data class WorkoutRestTimerState(
    val status: WorkoutRestTimerStatus = WorkoutRestTimerStatus.Idle,
    val targetElapsedRealtime: Long? = null,
    val remainingMillis: Long = 0L,
    val totalMillis: Long = 0L,
)

class WorkoutRestTimer(
    private val clock: MonotonicClock,
    private val scope: CoroutineScope,
    private val tickerEnabled: Boolean = true,
    private val tickerDelayMillis: Long = DEFAULT_TICKER_DELAY_MILLIS,
    private val onFinished: () -> Unit = {},
) {
    private val _state = MutableStateFlow(WorkoutRestTimerState())
    val state: StateFlow<WorkoutRestTimerState> = _state.asStateFlow()

    private var tickerJob: Job? = null

    fun start(restSeconds: Int): Boolean {
        if (restSeconds <= 0 || _state.value.status == WorkoutRestTimerStatus.Running) {
            return false
        }
        val totalMillis = restSeconds.toLong() * MILLIS_PER_SECOND
        val now = clock.elapsedRealtime()
        val target = now.toLongOrNullAdd(totalMillis) ?: return false
        stopTicker()
        _state.value = WorkoutRestTimerState(
            status = WorkoutRestTimerStatus.Running,
            targetElapsedRealtime = target,
            remainingMillis = totalMillis,
            totalMillis = totalMillis,
        )
        if (tickerEnabled) {
            tickerJob = scope.launch {
                while (isActive && _state.value.status == WorkoutRestTimerStatus.Running) {
                    refresh()
                    if (_state.value.status == WorkoutRestTimerStatus.Finished) break
                    delay(tickerDelayMillis)
                }
            }
        }
        return true
    }

    fun refresh() {
        val current = _state.value
        val target = current.targetElapsedRealtime ?: return
        if (current.status != WorkoutRestTimerStatus.Running) return
        val remaining = max(0L, target - clock.elapsedRealtime())
        if (remaining == 0L) {
            stopTicker()
            _state.value = current.copy(
                status = WorkoutRestTimerStatus.Finished,
                remainingMillis = 0L,
            )
            onFinished()
        } else {
            _state.value = current.copy(remainingMillis = remaining)
        }
    }

    fun stop() {
        stopTicker()
        _state.value = WorkoutRestTimerState()
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun Long.toLongOrNullAdd(value: Long): Long? =
        if (value > 0L && this > Long.MAX_VALUE - value) null else this + value
}

private const val MILLIS_PER_SECOND = 1_000L
private const val DEFAULT_TICKER_DELAY_MILLIS = 200L
