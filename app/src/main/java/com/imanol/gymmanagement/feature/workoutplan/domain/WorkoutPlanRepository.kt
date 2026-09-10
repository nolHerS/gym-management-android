package com.imanol.gymmanagement.feature.workoutplan.domain

interface WorkoutPlanRepository {
    suspend fun getMine(): List<WorkoutPlan>
    suspend fun getMyWeek(weekStart: String): List<WorkoutPlan>

    suspend fun create(clientId: Long, request: CreateWorkoutPlanRequest): WorkoutPlan
    suspend fun getForClient(clientId: Long): List<WorkoutPlan>
    suspend fun get(id: Long): WorkoutPlan
    suspend fun update(id: Long, request: UpdateWorkoutPlanRequest): WorkoutPlan
    suspend fun addDay(planId: Long, request: WorkoutPlanDayRequest): WorkoutPlan
    suspend fun deleteDay(planId: Long, day: Int)
    suspend fun addExercise(planId: Long, day: Int, request: WorkoutPlanExerciseRequest): WorkoutPlan
    suspend fun updateExercise(id: Long, request: WorkoutPlanExerciseRequest): WorkoutPlan
    suspend fun deleteExercise(id: Long)
    suspend fun deactivate(id: Long)
    suspend fun complete(id: Long)
}
