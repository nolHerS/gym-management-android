package com.imanol.gymmanagement.feature.workoutsession.data

import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.core.network.networkCall
import com.imanol.gymmanagement.feature.workoutsession.data.remote.WorkoutSessionApi
import com.imanol.gymmanagement.feature.workoutsession.data.remote.WorkoutSessionCreateRequestDto
import com.imanol.gymmanagement.feature.workoutsession.data.remote.toDomain
import com.imanol.gymmanagement.feature.workoutsession.data.remote.toDto
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSession
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionExercise
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionExerciseUpdate
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionRepository
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionSet
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionSetInput
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionUpdate
import javax.inject.Inject

class WorkoutSessionRepositoryImpl @Inject constructor(
    private val api: WorkoutSessionApi,
) : WorkoutSessionRepository {
    override suspend fun createSession(workoutPlanId: Long) = networkCall {
        api.createSession(WorkoutSessionCreateRequestDto(workoutPlanId)).data?.toDomain()
            ?: throw AppException.InvalidResponse("Workout session response did not contain data")
    }

    override suspend fun getMySessions() = networkCall {
        api.getMySessions().data?.map { it.toDomain() }
            ?: throw AppException.InvalidResponse("Workout sessions response did not contain data")
    }

    override suspend fun getSession(sessionId: Long) = networkCall {
        api.getSession(sessionId).data?.toDomain()
            ?: throw AppException.InvalidResponse("Workout session response did not contain data")
    }

    override suspend fun updateSession(sessionId: Long, update: WorkoutSessionUpdate) = networkCall {
        api.updateSession(sessionId, update.toDto()).data?.toDomain()
            ?: throw AppException.InvalidResponse("Updated workout session response did not contain data")
    }

    override suspend fun addSet(sessionId: Long, input: WorkoutSessionSetInput): WorkoutSessionSet = networkCall {
        api.addSet(sessionId, input.toDto()).data?.toDomain()
            ?: throw AppException.InvalidResponse("Workout session set response did not contain data")
    }

    override suspend fun updateExercise(
        sessionId: Long,
        exerciseId: Long,
        update: WorkoutSessionExerciseUpdate,
    ): WorkoutSessionExercise = networkCall {
        api.updateExercise(sessionId, exerciseId, update.toDto()).data?.toDomain()
            ?: throw AppException.InvalidResponse("Workout session exercise response did not contain data")
    }

    override suspend fun finishSession(sessionId: Long) = networkCall {
        api.finishSession(sessionId).data?.toDomain()
            ?: throw AppException.InvalidResponse("Finished workout session response did not contain data")
    }

    override suspend fun cancelSession(sessionId: Long) = networkCall {
        api.cancelSession(sessionId).data?.toDomain()
            ?: throw AppException.InvalidResponse("Cancelled workout session response did not contain data")
    }
}
