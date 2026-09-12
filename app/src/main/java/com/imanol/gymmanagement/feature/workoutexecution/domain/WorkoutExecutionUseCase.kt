package com.imanol.gymmanagement.feature.workoutexecution.domain

class WorkoutExecutionUseCase(snapshot: WorkoutExecutionSnapshot) {
    private val engine = WorkoutExecutionEngine(snapshot)

    val state: WorkoutExecutionState
        get() = engine.state

    val currentRestSeconds: Int
        get() = engine.currentRestSeconds

    fun start(): WorkoutExecutionState = engine.start()

    fun completeCurrentSet(): WorkoutExecutionState = engine.completeCurrentSet()

    fun finishRest(): WorkoutExecutionState = engine.finishRest()

    fun cancel(): WorkoutExecutionState = engine.cancel()

    fun finish(): WorkoutExecutionState = engine.finish()
}

private class WorkoutExecutionEngine(
    private val snapshot: WorkoutExecutionSnapshot,
) {
    var state: WorkoutExecutionState = initialState(snapshot)
        private set

    val currentRestSeconds: Int
        get() = snapshot.exercises.getOrNull(state.currentExerciseIndex)?.restSeconds ?: 0

    fun start(): WorkoutExecutionState {
        if (state.status == WorkoutExecutionStatus.Ready &&
            snapshot.exercises.isNotEmpty() &&
            state.currentExerciseTotalSets > 0
        ) {
            state = state.copy(status = WorkoutExecutionStatus.Running)
        }
        return state
    }

    fun completeCurrentSet(): WorkoutExecutionState {
        if (state.status != WorkoutExecutionStatus.Running) return state
        val exercise = snapshot.exercises.getOrNull(state.currentExerciseIndex) ?: return state
        if (state.currentSetNumber !in 1..exercise.sets.size) return state

        val completedSets = state.completedSets + 1
        state = state.copy(
            completedSets = completedSets,
            completedSetPositions = state.completedSetPositions +
                WorkoutExecutionSetPosition(
                    exerciseIndex = state.currentExerciseIndex,
                    setNumber = state.currentSetNumber,
                ),
        )
        if (exercise.restSeconds > 0) {
            state = state.copy(status = WorkoutExecutionStatus.Resting)
        } else {
            advanceAfterSet(completedSets)
        }
        return state
    }

    fun finishRest(): WorkoutExecutionState {
        if (state.status != WorkoutExecutionStatus.Resting) return state
        advanceAfterSet(state.completedSets)
        return state
    }

    fun cancel(): WorkoutExecutionState {
        if (state.status == WorkoutExecutionStatus.Ready ||
            state.status == WorkoutExecutionStatus.Running ||
            state.status == WorkoutExecutionStatus.Resting
        ) {
            state = state.copy(status = WorkoutExecutionStatus.Cancelled)
        }
        return state
    }

    fun finish(): WorkoutExecutionState {
        if (state.status == WorkoutExecutionStatus.Running ||
            state.status == WorkoutExecutionStatus.Resting
        ) {
            state = state.copy(status = WorkoutExecutionStatus.Finished)
        }
        return state
    }

    private fun advanceAfterSet(completedSets: Int) {
        val exercise = snapshot.exercises.getOrNull(state.currentExerciseIndex) ?: return
        if (state.currentSetNumber < exercise.sets.size) {
            state = state.copy(
                status = WorkoutExecutionStatus.Running,
                currentSetNumber = state.currentSetNumber + 1,
            )
            return
        }

        val nextExerciseIndex = state.currentExerciseIndex + 1
        if (nextExerciseIndex >= snapshot.exercises.size) {
            state = state.copy(
                status = WorkoutExecutionStatus.Finished,
                completedSets = completedSets,
            )
            return
        }
        val nextExercise = snapshot.exercises[nextExerciseIndex]
        state = state.copy(
            status = WorkoutExecutionStatus.Running,
            currentExerciseIndex = nextExerciseIndex,
            currentSetNumber = 1,
            currentExerciseTotalSets = nextExercise.sets.size,
            completedSets = completedSets,
        )
    }
}

private fun initialState(snapshot: WorkoutExecutionSnapshot): WorkoutExecutionState {
    val firstExercise = snapshot.exercises.firstOrNull()
    return WorkoutExecutionState(
        currentExerciseTotalSets = firstExercise?.sets?.size ?: 0,
        totalExercises = snapshot.exercises.size,
        totalSets = snapshot.exercises.sumOf { it.sets.size },
    )
}
