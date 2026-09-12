package com.imanol.gymmanagement.feature.workoutplan.domain

import java.util.UUID

enum class CreationMode { TEMPLATE, FROM_SCRATCH }

data class WorkoutPlanDraftExercise(
    val localId: Long,
    val request: WorkoutPlanExerciseRequest,
    val exerciseName: String? = null,
)

data class WorkoutPlanDraftDay(
    val dayOfWeek: Int,
    val localId: String = UUID.randomUUID().toString(),
    val exercises: List<WorkoutPlanDraftExercise> = emptyList(),
)

data class WorkoutPlanDraft(
    val clientId: Long = 0L,
    val startDate: String = "",
    val endDate: String? = null,
    val sourceTemplateId: Long? = null,
    val days: List<WorkoutPlanDraftDay> = emptyList(),
) {
    fun addDay(dayOfWeek: Int): WorkoutPlanDraft {
        if (dayOfWeek !in 1..7 || days.any { it.dayOfWeek == dayOfWeek }) return this
        return copy(days = (days + WorkoutPlanDraftDay(dayOfWeek)).sortedBy { it.dayOfWeek })
    }

    fun removeDay(localId: String): WorkoutPlanDraft =
        copy(days = days.filterNot { it.localId == localId })

    fun upsertExercise(dayLocalId: String, exercise: WorkoutPlanDraftExercise): WorkoutPlanDraft {
        val day = days.find { it.localId == dayLocalId } ?: return this
        val exercises = day.exercises
            .filterNot { it.localId == exercise.localId } + exercise
        val updatedDay = day.copy(exercises = exercises.sortedBy { it.request.orderIndex })
        return copy(
            days = days
                .filterNot { it.localId == dayLocalId }
                .plus(updatedDay)
                .sortedBy { it.dayOfWeek },
        )
    }

    fun removeExercise(localId: Long): WorkoutPlanDraft =
        copy(
            days = days.map { day ->
                day.copy(exercises = day.exercises.filterNot { it.localId == localId })
            },
        )

    fun toRequests(): List<WorkoutPlanDayRequest> = days
        .sortedBy { it.dayOfWeek }
        .map { day ->
            WorkoutPlanDayRequest(
                dayOfWeek = day.dayOfWeek,
                exercises = day.exercises
                    .sortedBy { it.request.orderIndex }
                    .map { it.request.copy(sourceTemplateExerciseId = null) },
            )
        }

    fun validationError(): String? {
        if (clientId <= 0L) return "El cliente no es válido."
        if (!isValidIsoDate(startDate)) return "La fecha de inicio no es válida."
        if (endDate != null && !isValidIsoDate(endDate)) return "La fecha de fin no es válida."
        if (endDate != null && endDate < startDate) return "La fecha de fin debe ser posterior o igual."
        if (sourceTemplateId != null) return "Un plan desde cero no puede tener plantilla."
        if (days.isEmpty()) return "Añade al menos un día."
        if (days.any { it.dayOfWeek !in 1..7 }) return "Hay días no válidos en el borrador."
        if (days.groupBy { it.dayOfWeek }.any { (_, grouped) -> grouped.size > 1 }) {
            return "Hay días duplicados en el borrador."
        }
        if (days.any { it.exercises.isEmpty() }) return "Añade al menos un ejercicio a cada día."
        days.forEach { day ->
            val requests = day.exercises.map { it.request }
            if (requests.any {
                    it.orderIndex < 1 || it.sets < 1 || it.repetitions < 1 || it.restSeconds < 0 ||
                        it.exerciseId == null || it.sourceTemplateExerciseId != null
                }
            ) return "Los datos del ejercicio no son válidos."
            if (requests.groupBy { it.orderIndex }.any { (_, grouped) -> grouped.size > 1 }) {
                return "El orden de ejercicios no puede repetirse en un mismo día."
            }
            val repeatedExercise = requests.mapNotNull { it.exerciseId }
                .groupBy { it }
                .any { (_, grouped) -> grouped.size > 1 }
            if (repeatedExercise) return "El ejercicio ya existe en este día."
        }
        return null
    }
}

private fun isValidIsoDate(value: String): Boolean =
    Regex("""\d{4}-\d{2}-\d{2}""").matches(value)
