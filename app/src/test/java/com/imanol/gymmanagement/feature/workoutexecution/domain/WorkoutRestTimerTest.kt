package com.imanol.gymmanagement.feature.workoutexecution.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutRestTimerTest {
    @Test
    fun startsWithMonotonicTargetAndCalculatesRemainingFromClock() {
        val clock = FakeMonotonicClock(1_000L)
        val timer = timer(clock)

        assertTrue(timer.start(90))
        assertEquals(91_000L, timer.state.value.targetElapsedRealtime)
        assertEquals(90_000L, timer.state.value.remainingMillis)

        clock.now = 21_000L
        timer.refresh()

        assertEquals(70_000L, timer.state.value.remainingMillis)
    }

    @Test
    fun finishesWhenClockReachesTargetAndCallsCallbackOnce() {
        val clock = FakeMonotonicClock(1_000L)
        var callbackCount = 0
        val timer = WorkoutRestTimer(
            clock = clock,
            scope = CoroutineScope(Dispatchers.Unconfined),
            tickerEnabled = false,
            onFinished = { callbackCount++ },
        )
        timer.start(1)
        clock.now = 2_000L

        timer.refresh()
        timer.refresh()

        assertEquals(WorkoutRestTimerStatus.Finished, timer.state.value.status)
        assertEquals(0L, timer.state.value.remainingMillis)
        assertEquals(1, callbackCount)
    }

    @Test
    fun duplicateStartDoesNotResetTarget() {
        val clock = FakeMonotonicClock(1_000L)
        val timer = timer(clock)
        timer.start(90)
        clock.now = 2_000L

        assertFalse(timer.start(120))
        assertEquals(91_000L, timer.state.value.targetElapsedRealtime)
        timer.refresh()
        assertEquals(89_000L, timer.state.value.remainingMillis)
    }

    @Test
    fun stopClearsTimerAndExpiredTargetDoesNotNeedRealWaiting() {
        val clock = FakeMonotonicClock(1_000L)
        val timer = timer(clock)
        timer.start(1)
        clock.now = 10_000L
        timer.refresh()
        assertEquals(WorkoutRestTimerStatus.Finished, timer.state.value.status)

        timer.stop()
        assertEquals(WorkoutRestTimerStatus.Idle, timer.state.value.status)
        assertEquals(null, timer.state.value.targetElapsedRealtime)
    }

    @Test
    fun zeroNegativeAndOverflowValuesAreRejected() {
        val clock = FakeMonotonicClock(1_000L)
        val timer = timer(clock)

        assertFalse(timer.start(0))
        assertFalse(timer.start(-1))

        clock.now = Long.MAX_VALUE
        assertFalse(timer.start(1))
        assertEquals(WorkoutRestTimerStatus.Idle, timer.state.value.status)
    }

    @Test
    fun elapsedTimeInBackgroundIsAccountedForByTargetCalculation() {
        val clock = FakeMonotonicClock(10_000L)
        val timer = timer(clock)
        timer.start(90)

        clock.now = 30_000L
        timer.refresh()

        assertEquals(70_000L, timer.state.value.remainingMillis)
    }

    private fun timer(clock: FakeMonotonicClock) = WorkoutRestTimer(
        clock = clock,
        scope = CoroutineScope(Dispatchers.Unconfined),
        tickerEnabled = false,
    )
}

private class FakeMonotonicClock(
    var now: Long,
) : MonotonicClock {
    override fun elapsedRealtime(): Long = now
}
