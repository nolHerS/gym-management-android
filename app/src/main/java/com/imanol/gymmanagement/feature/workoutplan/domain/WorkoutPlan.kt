package com.imanol.gymmanagement.feature.workoutplan.domain

import com.imanol.gymmanagement.feature.exercise.domain.Exercise

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
        runCatching { java.time.LocalDate.parse(startDate) }.isFailure ->
            "La fecha de inicio no es válida."
        endDate != null && runCatching { java.time.LocalDate.parse(endDate) }.isFailure ->
            "La fecha de fin no es válida."
        endDate != null && java.time.LocalDate.parse(endDate)
            .isBefore(java.time.LocalDate.parse(startDate)) ->
            "La fecha de fin debe ser posterior o igual."
        days.isEmpty() || days.any { it.dayOfWeek !in 1..7 || it.exercises.isEmpty() } -> "Añade al menos un ejercicio a cada día."
        days.flatMap { it.exercises }.any { it.orderIndex < 1 || it.sets < 1 || it.repetitions < 1 || it.restSeconds < 0 || (it.exerciseId == null && it.sourceTemplateExerciseId == null) } -> "Revisa los valores de los ejercicios."
        else -> null
    }
}
data class UpdateWorkoutPlanRequest(val startDate: String, val endDate: String?, val status: String)
