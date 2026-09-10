package com.imanol.gymmanagement.feature.workoutplan.data.remote

import com.imanol.gymmanagement.feature.exercise.data.remote.ExerciseResponse
import com.imanol.gymmanagement.feature.exercise.data.remote.toDomain
import com.imanol.gymmanagement.feature.workoutplan.domain.*
import kotlinx.serialization.Serializable

@Serializable data class WorkoutPlanRequest(val sourceTemplateId: Long? = null, val startDate: String, val endDate: String? = null, val days: List<DayRequest>)
@Serializable data class DayRequest(val dayOfWeek: Int, val exercises: List<ExerciseRequest> = emptyList())
@Serializable data class ExerciseRequest(val sourceTemplateExerciseId: Long? = null, val exerciseId: Long? = null, val orderIndex: Int, val sets: Int, val repetitions: Int, val restSeconds: Int)
@Serializable data class RemoteUpdateWorkoutPlanRequest(val startDate: String, val endDate: String?, val status: String)
@Serializable data class WorkoutPlanResponse(val id: Long, val clientId: Long, val trainerId: Long, val sourceTemplateId: Long? = null, val startDate: String, val endDate: String? = null, val status: String, val days: List<WorkoutPlanDayResponse> = emptyList())
@Serializable data class WorkoutPlanDayResponse(val id: Long, val dayOfWeek: Int, val exercises: List<WorkoutPlanExerciseResponse> = emptyList())
@Serializable data class WorkoutPlanExerciseResponse(val id: Long, val exercise: ExerciseResponse? = null, val sourceTemplateExerciseId: Long? = null, val orderIndex: Int, val sets: Int, val repetitions: Int, val restSeconds: Int)

fun WorkoutPlanRequest.validationError(): String? = when {
    !Regex("""\d{4}-\d{2}-\d{2}""").matches(startDate) -> "La fecha de inicio no es válida."
    days.isEmpty() -> "El plan debe tener al menos un día."
    days.any { it.dayOfWeek !in 1..7 || it.exercises.isEmpty() } -> "Cada día debe tener ejercicios."
    days.flatMap { it.exercises }.any {
        it.orderIndex < 1 || it.sets < 1 || it.repetitions < 1 || it.restSeconds < 0 ||
            (it.exerciseId == null && it.sourceTemplateExerciseId == null)
    } -> "Los ejercicios del plan no son válidos."
    else -> null
}
fun CreateWorkoutPlanRequest.toRemote() = WorkoutPlanRequest(sourceTemplateId, startDate, endDate, days.map { DayRequest(it.dayOfWeek, it.exercises.map { e -> ExerciseRequest(e.sourceTemplateExerciseId, e.exerciseId, e.orderIndex, e.sets, e.repetitions, e.restSeconds) }) })

fun WorkoutPlanResponse.toDomain() = WorkoutPlan(id, clientId, trainerId, sourceTemplateId, startDate, endDate, status, days.map { it.toDomain() })
fun WorkoutPlanDayResponse.toDomain() = WorkoutPlanDay(id, dayOfWeek, exercises.map { it.toDomain() })
fun WorkoutPlanExerciseResponse.toDomain() = WorkoutPlanExercise(id, exercise?.toDomain(), sourceTemplateExerciseId, orderIndex, sets, repetitions, restSeconds)
