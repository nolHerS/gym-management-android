package com.imanol.gymmanagement.feature.workoutsession.data

import com.imanol.gymmanagement.core.network.model.ApiResponse
import com.imanol.gymmanagement.feature.workoutsession.data.remote.WorkoutSessionApi
import com.imanol.gymmanagement.feature.workoutsession.data.remote.WorkoutSessionCreateRequestDto
import com.imanol.gymmanagement.feature.workoutsession.data.remote.WorkoutSessionExerciseResponseDto
import com.imanol.gymmanagement.feature.workoutsession.data.remote.WorkoutSessionExerciseUpdateRequestDto
import com.imanol.gymmanagement.feature.workoutsession.data.remote.WorkoutSessionResponseDto
import com.imanol.gymmanagement.feature.workoutsession.data.remote.WorkoutSessionSetRequestDto
import com.imanol.gymmanagement.feature.workoutsession.data.remote.WorkoutSessionSetResponseDto
import com.imanol.gymmanagement.feature.workoutsession.data.remote.WorkoutSessionUpdateRequestDto
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionExerciseUpdate
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionSetInput
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionUpdate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSessionRepositoryImplTest {
    @Test
    fun delegatesSessionOperationsAndMapsResponses() = runBlocking {
        val api = RecordingWorkoutSessionApi()
        val repository = WorkoutSessionRepositoryImpl(api)

        assertEquals(1L, repository.createSession(2L).id)
        assertEquals(1, repository.getMySessions().size)
        assertEquals(1L, repository.getSession(1L).id)
        assertEquals(1L, repository.updateSession(1L, WorkoutSessionUpdate("done", 60)).id)
        assertEquals(7L, repository.addSet(1L, WorkoutSessionSetInput(4L, 1)).id)
        assertEquals(4L, repository.updateExercise(1L, 4L, WorkoutSessionExerciseUpdate(true, "ok")).id)
        assertEquals(1L, repository.finishSession(1L).id)
        assertEquals(1L, repository.cancelSession(1L).id)

        assertEquals(2L, api.createdPlanId)
        assertEquals("done", api.updated?.notes)
        assertEquals(4L, api.updatedExerciseId)
    }
}

private class RecordingWorkoutSessionApi : WorkoutSessionApi {
    var createdPlanId: Long? = null
    var updated: WorkoutSessionUpdateRequestDto? = null
    var updatedExerciseId: Long? = null

    private val set = WorkoutSessionSetResponseDto(7L, 1, 10, null, null, null, null, false, null)
    private val exercise = WorkoutSessionExerciseResponseDto(4L, 5L, "Press", 1, 3, 10, 60, false, null, listOf(set))
    private val session = WorkoutSessionResponseDto(1L, 2L, 3L, "IN_PROGRESS", "2026-09-12T10:00:00", exercises = listOf(exercise), version = 0L)

    override suspend fun createSession(request: WorkoutSessionCreateRequestDto): ApiResponse<WorkoutSessionResponseDto> {
        createdPlanId = request.workoutPlanId
        return response(session)
    }

    override suspend fun getMySessions() = response(listOf(session))
    override suspend fun getSession(sessionId: Long) = response(session)

    override suspend fun updateSession(
        sessionId: Long,
        request: WorkoutSessionUpdateRequestDto,
    ): ApiResponse<WorkoutSessionResponseDto> {
        updated = request
        return response(session)
    }

    override suspend fun addSet(sessionId: Long, request: WorkoutSessionSetRequestDto) = response(set)

    override suspend fun updateExercise(
        sessionId: Long,
        exerciseId: Long,
        request: WorkoutSessionExerciseUpdateRequestDto,
    ): ApiResponse<WorkoutSessionExerciseResponseDto> {
        updatedExerciseId = exerciseId
        return response(exercise)
    }

    override suspend fun finishSession(sessionId: Long) = response(session)
    override suspend fun cancelSession(sessionId: Long) = response(session)

    private fun <T> response(data: T) = ApiResponse("2026-09-12T10:00:00", 200, null, "OK", data)
}
