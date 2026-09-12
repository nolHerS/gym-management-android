package com.imanol.gymmanagement.feature.workoutplan.data

import com.imanol.gymmanagement.core.network.model.ApiResponse
import com.imanol.gymmanagement.feature.exercise.data.remote.ExerciseResponse
import com.imanol.gymmanagement.feature.workoutplan.data.remote.DayRequest
import com.imanol.gymmanagement.feature.workoutplan.data.remote.ExerciseRequest
import com.imanol.gymmanagement.feature.workoutplan.data.remote.RemoteUpdateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.data.remote.WorkoutPlanApi
import com.imanol.gymmanagement.feature.workoutplan.data.remote.WorkoutPlanDayResponse
import com.imanol.gymmanagement.feature.workoutplan.data.remote.WorkoutPlanExerciseResponse
import com.imanol.gymmanagement.feature.workoutplan.data.remote.WorkoutPlanResponse
import com.imanol.gymmanagement.feature.workoutplan.data.remote.WorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.CreateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.UpdateWorkoutPlanRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDayRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExerciseRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPlanRepositoryImplTest {
    @Test
    fun mapsDayAndExerciseMutationsToTheirResponseTypes() = runBlocking {
        val api = RecordingWorkoutPlanApi()
        val repository = WorkoutPlanRepositoryImpl(api)
        val exerciseRequest = WorkoutPlanExerciseRequest(
            exerciseId = 3L,
            orderIndex = 1,
            sets = 3,
            repetitions = 10,
            restSeconds = 60,
        )

        val day = repository.addDay(8L, WorkoutPlanDayRequest(1, listOf(exerciseRequest)))
        val added = repository.addExercise(8L, 1, exerciseRequest)
        val updated = repository.updateExercise(10L, exerciseRequest.copy(sets = 4))
        repository.deleteDay(8L, 1)
        repository.deleteExercise(10L)

        assertEquals(9L, day.id)
        assertEquals(10L, added.id)
        assertEquals(4, updated.sets)
        assertTrue(api.deletedDay)
        assertTrue(api.deletedExercise)
    }

    @Test
    fun mapsPlanUpdateAndStatusMutations() = runBlocking {
        val api = RecordingWorkoutPlanApi()
        val repository = WorkoutPlanRepositoryImpl(api)

        val updated = repository.update(
            8L,
            UpdateWorkoutPlanRequest(status = "COMPLETED"),
        )
        repository.complete(8L)
        repository.deactivate(8L)

        assertEquals(8L, updated.id)
        assertEquals("COMPLETED", api.lastUpdate?.status)
        assertTrue(api.completed)
        assertTrue(api.deactivated)
    }
}

private class RecordingWorkoutPlanApi : WorkoutPlanApi {
    var deletedDay = false
    var deletedExercise = false
    var completed = false
    var deactivated = false
    var lastUpdate: RemoteUpdateWorkoutPlanRequest? = null

    private val exercise = WorkoutPlanExerciseResponse(
        id = 10L,
        exercise = ExerciseResponse(3L, "Press", null, 5L, "Chest", true),
        sourceTemplateExerciseId = null,
        orderIndex = 1,
        sets = 4,
        repetitions = 10,
        restSeconds = 60,
    )

    private val day = WorkoutPlanDayResponse(9L, 1, listOf(exercise))
    private val plan = WorkoutPlanResponse(
        id = 8L,
        clientId = 2L,
        trainerId = 1L,
        sourceTemplateId = null,
        startDate = "2026-09-01",
        endDate = null,
        status = "ACTIVE",
        days = listOf(day),
    )

    override suspend fun getMine() = response(listOf(plan))
    override suspend fun getMyWeek(weekStart: String) = response(listOf(plan))
    override suspend fun create(clientId: Long, request: WorkoutPlanRequest) = response(plan)
    override suspend fun getForClient(clientId: Long) = response(listOf(plan))
    override suspend fun get(id: Long) = response(plan)

    override suspend fun update(
        id: Long,
        request: RemoteUpdateWorkoutPlanRequest,
    ): ApiResponse<WorkoutPlanResponse> {
        lastUpdate = request
        return response(plan)
    }

    override suspend fun addDay(
        planId: Long,
        request: DayRequest,
    ) = response(day)

    override suspend fun deleteDay(planId: Long, day: Int): ApiResponse<Unit> {
        deletedDay = true
        return response(null)
    }

    override suspend fun addExercise(
        planId: Long,
        day: Int,
        request: ExerciseRequest,
    ) = response(exercise)

    override suspend fun updateExercise(
        id: Long,
        request: ExerciseRequest,
    ) = response(exercise)

    override suspend fun deleteExercise(id: Long): ApiResponse<Unit> {
        deletedExercise = true
        return response(null)
    }

    override suspend fun deactivate(id: Long): ApiResponse<Unit> {
        deactivated = true
        return response(null)
    }

    override suspend fun complete(id: Long): ApiResponse<Unit> {
        completed = true
        return response(null)
    }

    private fun <T> response(data: T?) = ApiResponse(
        timestamp = "2026-09-12T10:00:00",
        status = 200,
        error = null,
        message = "OK",
        data = data,
    )
}
