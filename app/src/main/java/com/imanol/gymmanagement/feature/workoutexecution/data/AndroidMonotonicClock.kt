package com.imanol.gymmanagement.feature.workoutexecution.data

import android.os.SystemClock
import com.imanol.gymmanagement.feature.workoutexecution.domain.MonotonicClock

class AndroidMonotonicClock : MonotonicClock {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}
