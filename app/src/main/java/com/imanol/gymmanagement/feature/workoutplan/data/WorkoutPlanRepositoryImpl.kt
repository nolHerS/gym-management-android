package com.imanol.gymmanagement.feature.workoutplan.data

import com.imanol.gymmanagement.feature.workoutplan.data.remote.*
import com.imanol.gymmanagement.feature.workoutplan.domain.*
import javax.inject.Inject
import com.imanol.gymmanagement.feature.workoutplan.data.remote.toRemote

class WorkoutPlanRepositoryImpl @Inject constructor(private val api: WorkoutPlanApi) : WorkoutPlanRepository {
    override suspend fun getMine() =
        api.getMine().data?.map { it.toDomain() } ?: emptyList()

    override suspend fun getMyWeek(weekStart: String) =
        api.getMyWeek(weekStart).data?.map { it.toDomain() } ?: emptyList()

    override suspend fun create(clientId: Long, request: CreateWorkoutPlanRequest) = api.create(clientId, request.toRemote()).data?.toDomain() ?: error("Missing workout plan")
    override suspend fun getForClient(clientId: Long) = api.getForClient(clientId).data?.map { it.toDomain() } ?: emptyList()
    override suspend fun get(id: Long) = api.get(id).data?.toDomain() ?: error("Missing workout plan")
    override suspend fun update(id: Long, request: UpdateWorkoutPlanRequest) = api.update(id, RemoteUpdateWorkoutPlanRequest(request.startDate, request.endDate, request.status)).data?.toDomain() ?: error("Missing workout plan")
    override suspend fun addDay(planId: Long, request: WorkoutPlanDayRequest) = api.addDay(planId, DayRequest(request.dayOfWeek, request.exercises.map { ExerciseRequest(it.sourceTemplateExerciseId, it.exerciseId, it.orderIndex, it.sets, it.repetitions, it.restSeconds) })).data?.toDomain() ?: error("Missing workout plan")
    override suspend fun addExercise(planId: Long, day: Int, request: WorkoutPlanExerciseRequest) = api.addExercise(planId, day, ExerciseRequest(request.sourceTemplateExerciseId, request.exerciseId, request.orderIndex, request.sets, request.repetitions, request.restSeconds)).data?.toDomain() ?: error("Missing workout plan")
    override suspend fun updateExercise(id: Long, request: WorkoutPlanExerciseRequest) = api.updateExercise(id, ExerciseRequest(request.sourceTemplateExerciseId, request.exerciseId, request.orderIndex, request.sets, request.repetitions, request.restSeconds)).data?.toDomain() ?: error("Missing workout plan")
    override suspend fun deleteExercise(id: Long) { api.deleteExercise(id) }
    override suspend fun deleteDay(planId: Long, day: Int) { api.deleteDay(planId, day) }
    override suspend fun deactivate(id: Long) { api.deactivate(id) }
    override suspend fun complete(id: Long) { api.complete(id) }
}
