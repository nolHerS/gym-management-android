package com.imanol.gymmanagement.feature.workoutsession.data

import com.imanol.gymmanagement.feature.workoutsession.data.remote.WorkoutSessionExerciseResponseDto
import com.imanol.gymmanagement.feature.workoutsession.data.remote.WorkoutSessionResponseDto
import com.imanol.gymmanagement.feature.workoutsession.data.remote.WorkoutSessionSetResponseDto
import com.imanol.gymmanagement.feature.workoutsession.data.remote.toDomain
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutSessionMappersTest {
    @Test
    fun mapsSessionExerciseSetStatusAndNullableExecutionFields() {
        val response = WorkoutSessionResponseDto(
            id = 1L,
            workoutPlanId = 2L,
            clientId = 3L,
            status = "IN_PROGRESS",
            startedAt = "2026-09-12T10:00:00",
            finishedAt = null,
            cancelledAt = null,
            durationSeconds = null,
            notes = null,
            version = 0L,
            exercises = listOf(
                WorkoutSessionExerciseResponseDto(
                    id = 4L,
                    exerciseId = 5L,
                    exerciseName = "Press",
                    orderIndex = 1,
                    plannedSets = 3,
                    plannedRepetitions = 10,
                    plannedRestSeconds = 60,
                    completed = false,
                    notes = null,
                    sets = listOf(
                        WorkoutSessionSetResponseDto(
                            id = 6L,
                            setNumber = 1,
                            plannedRepetitions = 10,
                            actualRepetitions = null,
                            weight = null,
                            rir = null,
                            rpe = null,
                            completed = false,
                            performedAt = null,
                        ),
                    ),
                ),
            ),
        )

        val domain = response.toDomain()

        assertEquals(WorkoutSessionStatus.IN_PROGRESS, domain.status)
        assertEquals("Press", domain.exercises.single().exerciseName)
        assertNull(domain.finishedAt)
        assertNull(domain.durationSeconds)
        assertNull(domain.exercises.single().sets.single().weight)
        assertNull(domain.exercises.single().sets.single().actualRepetitions)
        assertNull(domain.exercises.single().sets.single().rir)
        assertNull(domain.exercises.single().sets.single().rpe)
        assertNull(domain.exercises.single().sets.single().performedAt)
    }

    @Test
    fun mapsAllStatuses() {
        assertEquals(WorkoutSessionStatus.COMPLETED, "COMPLETED".toDomain())
        assertEquals(WorkoutSessionStatus.CANCELLED, "CANCELLED".toDomain())
    }
}
