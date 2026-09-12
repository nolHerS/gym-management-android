package com.imanol.gymmanagement.feature.workoutplan.domain

import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

data class WorkoutPlan(val id: Long, val clientId: Long, val trainerId: Long, val sourceTemplateId: Long?, val startDate: String, val endDate: String?, val status: String, val days: List<WorkoutPlanDay>)
data class WorkoutPlanDay(val id: Long, val dayOfWeek: Int, val exercises: List<WorkoutPlanExercise>)
data class WorkoutPlanExercise(val id: Long, val exercise: Exercise?, val sourceTemplateExerciseId: Long?, val orderIndex: Int, val sets: Int, val repetitions: Int, val restSeconds: Int)
data class WorkoutPlanExerciseRequest(
    val sourceTemplateExerciseId: Long? = null,
    val exerciseId: Long? = null,
    val orderIndex: Int,
    val sets: Int,
    val repetitions: Int,
    val restSeconds: Int,
)
data class WorkoutPlanDayRequest(val dayOfWeek: Int, val exercises: List<WorkoutPlanExerciseRequest>)
data class CreateWorkoutPlanRequest(
    val sourceTemplateId: Long? = null,
    val startDate: String,
    val endDate: String? = null,
    val days: List<WorkoutPlanDayRequest>,
) {
    fun validationError(): String? = when {
        parseIsoDate(startDate) == null ->
            "La fecha de inicio no es válida."
        endDate != null && parseIsoDate(endDate) == null ->
            "La fecha de fin no es válida."
        endDate != null && parseIsoDate(endDate)!!.before(parseIsoDate(startDate)) ->
            "La fecha de fin debe ser posterior o igual."
        days.groupBy { it.dayOfWeek }.any { (_, grouped) -> grouped.size > 1 } ->
            "No puede haber días duplicados."
        days.isEmpty() || days.any { it.dayOfWeek !in 1..7 || it.exercises.isEmpty() } -> "Añade al menos un ejercicio a cada día."
        days.flatMap { it.exercises }.any { it.orderIndex < 1 || it.sets < 1 || it.repetitions < 1 || it.restSeconds < 0 || (it.exerciseId == null && it.sourceTemplateExerciseId == null) } -> "Revisa los valores de los ejercicios."
        days.any { day ->
            day.exercises.groupBy { it.orderIndex }.any { (_, grouped) -> grouped.size > 1 }
        } -> "El orden de ejercicios no puede repetirse en un mismo día."
        days.any { day ->
            day.exercises.mapNotNull { it.exerciseId }.groupBy { it }.any { (_, grouped) -> grouped.size > 1 }
        } -> "El ejercicio ya existe en este día."
        else -> null
    }
}

private fun parseIsoDate(value: String) =
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }.let { format ->
        ParsePosition(0).let { position ->
            format.parse(value, position)?.takeIf { position.index == value.length }
        }
    }
data class UpdateWorkoutPlanRequest(
    val startDate: String? = null,
    val endDate: String? = null,
    val status: String? = null,
)
