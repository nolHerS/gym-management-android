package com.imanol.gymmanagement.feature.workoutplan.data

import com.imanol.gymmanagement.feature.workoutplan.data.remote.*
import com.imanol.gymmanagement.feature.workoutplan.domain.*
import javax.inject.Inject
import com.imanol.gymmanagement.feature.workoutplan.data.remote.toRemote
import com.imanol.gymmanagement.core.network.networkCall
import com.imanol.gymmanagement.core.domain.AppException

class WorkoutPlanRepositoryImpl @Inject constructor(private val api: WorkoutPlanApi) : WorkoutPlanRepository {
    override suspend fun getMine() =
        networkCall { api.getMine().data?.map { it.toDomain() } ?: throw AppException.InvalidResponse("Missing workout plans") }

    override suspend fun getMyWeek(weekStart: String) =
        networkCall { api.getMyWeek(weekStart).data?.map { it.toDomain() } ?: throw AppException.InvalidResponse("Missing workout plans") }

    override suspend fun create(clientId: Long, request: CreateWorkoutPlanRequest) = networkCall { api.create(clientId, request.toRemote()).data?.toDomain() ?: throw AppException.InvalidResponse("Missing workout plan") }
    override suspend fun getForClient(clientId: Long) = networkCall { api.getForClient(clientId).data?.map { it.toDomain() } ?: throw AppException.InvalidResponse("Missing workout plans") }
    override suspend fun get(id: Long) = networkCall { api.get(id).data?.toDomain() ?: throw AppException.InvalidResponse("Missing workout plan") }
    override suspend fun update(id: Long, request: UpdateWorkoutPlanRequest) = networkCall {
        api.update(
            id,
            RemoteUpdateWorkoutPlanRequest(
                startDate = request.startDate,
                endDate = request.endDate,
                status = request.status,
            ),
        ).data?.toDomain() ?: throw AppException.InvalidResponse("Missing workout plan")
    }
    override suspend fun addDay(planId: Long, request: WorkoutPlanDayRequest) = networkCall {
        api.addDay(
            planId,
            DayRequest(
                request.dayOfWeek,
                request.exercises.map {
                    ExerciseRequest(
                        it.sourceTemplateExerciseId,
                        it.exerciseId,
                        it.orderIndex,
                        it.sets,
                        it.repetitions,
                        it.restSeconds,
                    )
                },
            ),
        ).data?.toDomain() ?: throw AppException.InvalidResponse("Missing workout plan day")
    }

    override suspend fun addExercise(
        planId: Long,
        day: Int,
        request: WorkoutPlanExerciseRequest,
    ) = networkCall {
        api.addExercise(
            planId,
            day,
            ExerciseRequest(
                request.sourceTemplateExerciseId,
                request.exerciseId,
                request.orderIndex,
                request.sets,
                request.repetitions,
                request.restSeconds,
            ),
        ).data?.toDomain() ?: throw AppException.InvalidResponse("Missing workout plan exercise")
    }

    override suspend fun updateExercise(
        id: Long,
        request: WorkoutPlanExerciseRequest,
    ) = networkCall {
        api.updateExercise(
            id,
            ExerciseRequest(
                request.sourceTemplateExerciseId,
                request.exerciseId,
                request.orderIndex,
                request.sets,
                request.repetitions,
                request.restSeconds,
            ),
        ).data?.toDomain() ?: throw AppException.InvalidResponse("Missing workout plan exercise")
    }
    override suspend fun deleteExercise(id: Long) { networkCall { api.deleteExercise(id) } }
    override suspend fun deleteDay(planId: Long, day: Int) { networkCall { api.deleteDay(planId, day) } }
    override suspend fun deactivate(id: Long) { networkCall { api.deactivate(id) } }
    override suspend fun complete(id: Long) { networkCall { api.complete(id) } }
}
