package com.imanol.gymmanagement.feature.workoutexecution.domain

import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDay
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExercise
import javax.inject.Inject

enum class WorkoutExecutionValidationReason {
    InvalidPlanId,
    InvalidClientId,
    PlanNotActive,
    PlanOutsideDateRange,
    InvalidDay,
    DayNotFound,
    DayHasNoExercises,
    InvalidExercise,
    InvalidExerciseOrder,
}

class InvalidWorkoutExecutionException(
    val reason: WorkoutExecutionValidationReason,
) : IllegalArgumentException("Invalid workout execution: $reason")

sealed interface WorkoutExecutionPlanSelection {
    data object NoCandidate : WorkoutExecutionPlanSelection
    data class Selected(val plan: WorkoutPlan) : WorkoutExecutionPlanSelection
    data class Ambiguous(val plans: List<WorkoutPlan>) : WorkoutExecutionPlanSelection
}

class SelectWorkoutPlanForExecutionUseCase @Inject constructor(
    private val dateProvider: WorkoutExecutionDateProvider,
) {
    operator fun invoke(
        plans: List<WorkoutPlan>,
        dayOfWeek: Int = isoDayOfWeek(dateProvider.todayIsoDate()),
    ): WorkoutExecutionPlanSelection {
        val today = dateProvider.todayIsoDate()
        val candidates = plans.filter { plan ->
            plan.status == ACTIVE_STATUS &&
                planAppliesOn(plan, today) &&
                plan.days.any { day -> day.dayOfWeek == dayOfWeek && day.exercises.isNotEmpty() }
        }
        return when (candidates.size) {
            0 -> WorkoutExecutionPlanSelection.NoCandidate
            1 -> WorkoutExecutionPlanSelection.Selected(candidates.single())
            else -> WorkoutExecutionPlanSelection.Ambiguous(candidates.toList())
        }
    }
}

class CreateWorkoutExecutionSnapshotUseCase @Inject constructor(
    private val dateProvider: WorkoutExecutionDateProvider,
) {
    operator fun invoke(
        plan: WorkoutPlan,
        dayOfWeek: Int = isoDayOfWeek(dateProvider.todayIsoDate()),
    ): WorkoutExecutionSnapshot {
        validate(plan, dayOfWeek)
        val day = plan.days.single { it.dayOfWeek == dayOfWeek }
        val exercises = day.exercises
            .sortedBy { it.orderIndex }
            .map { exercise ->
                WorkoutExecutionExercise(
                    exerciseId = requireNotNull(exercise.exercise).id,
                    name = requireNotNull(exercise.exercise).name,
                    sets = (1..exercise.sets).map { setNumber ->
                        WorkoutExecutionSet(
                            setNumber = setNumber,
                            plannedRepetitions = exercise.repetitions,
                        )
                    }.toList(),
                    restSeconds = exercise.restSeconds,
                    orderIndex = exercise.orderIndex,
                )
            }.toList()
        return WorkoutExecutionSnapshot(
            planId = plan.id,
            clientId = plan.clientId,
            startedAt = dateProvider.nowEpochMillis(),
            exercises = exercises,
        )
    }

    fun validate(plan: WorkoutPlan, dayOfWeek: Int) {
        when {
            plan.id <= 0L -> invalid(WorkoutExecutionValidationReason.InvalidPlanId)
            plan.clientId <= 0L -> invalid(WorkoutExecutionValidationReason.InvalidClientId)
            plan.status != ACTIVE_STATUS -> invalid(WorkoutExecutionValidationReason.PlanNotActive)
            !planAppliesOn(plan, dateProvider.todayIsoDate()) ->
                invalid(WorkoutExecutionValidationReason.PlanOutsideDateRange)
            dayOfWeek !in 1..7 -> invalid(WorkoutExecutionValidationReason.InvalidDay)
        }
        val matchingDays = plan.days.filter { it.dayOfWeek == dayOfWeek }
        if (matchingDays.size != 1) {
            invalid(WorkoutExecutionValidationReason.DayNotFound)
        }
        val day = matchingDays.single()
        if (day.exercises.isEmpty()) {
            invalid(WorkoutExecutionValidationReason.DayHasNoExercises)
        }
        if (day.exercises.any { it.orderIndex < 1 }) {
            invalid(WorkoutExecutionValidationReason.InvalidExerciseOrder)
        }
        if (day.exercises.map { it.orderIndex }.toSet().size != day.exercises.size) {
            invalid(WorkoutExecutionValidationReason.InvalidExerciseOrder)
        }
        if (day.exercises.any(::hasInvalidExercise)) {
            invalid(WorkoutExecutionValidationReason.InvalidExercise)
        }
    }

    private fun hasInvalidExercise(exercise: WorkoutPlanExercise): Boolean =
        exercise.exercise == null ||
            exercise.exercise.id <= 0L ||
            exercise.exercise.name.isBlank() ||
            exercise.sets < 1 ||
            exercise.repetitions < 1 ||
            exercise.restSeconds < 0

    private fun invalid(reason: WorkoutExecutionValidationReason): Nothing =
        throw InvalidWorkoutExecutionException(reason)
}

private fun planAppliesOn(plan: WorkoutPlan, date: String): Boolean =
    isValidIsoDate(plan.startDate) &&
        plan.startDate <= date &&
        (plan.endDate == null || (isValidIsoDate(plan.endDate) && date <= plan.endDate))

private fun isValidIsoDate(value: String): Boolean = try {
    isoDayOfWeek(value)
    true
} catch (_: IllegalArgumentException) {
    false
}

private const val ACTIVE_STATUS = "ACTIVE"
