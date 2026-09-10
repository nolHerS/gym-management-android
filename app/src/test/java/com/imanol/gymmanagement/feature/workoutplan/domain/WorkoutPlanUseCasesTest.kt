package com.imanol.gymmanagement.feature.workoutplan.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutPlanUseCasesTest {
    private val plan = WorkoutPlan(1L, 2L, 3L, null, "2026-09-01", null, "ACTIVE", emptyList())

    @Test
    fun getMyPlanDelegatesToRepository() = runBlocking {
        val repository = FakeRepository().also { it.plans = listOf(plan) }
        assertEquals(listOf(plan), GetMyWorkoutPlanUseCase(repository)())
        assertEquals(listOf(plan), GetMyWorkoutWeekUseCase(repository)("2026-09-07"))
    }

    @Test
    fun getClientPlansDelegatesToRepository() = runBlocking {
        val repository = FakeRepository().also { it.plans = listOf(plan) }
        assertEquals(listOf(plan), GetClientWorkoutPlansUseCase(repository)(2L))
    }

    @Test
    fun getDetailDelegatesToRepository() = runBlocking {
        val repository = FakeRepository().also { it.plan = plan }
        assertEquals(plan, GetWorkoutPlanDetailUseCase(repository)(1L))
    }

    @Test
    fun createDelegatesAfterValidation() = runBlocking {
        val repository = FakeRepository()
        val request = CreateWorkoutPlanRequest(
            startDate = "2026-09-01",
            days = listOf(
                WorkoutPlanDayRequest(
                    1,
                    listOf(
                        WorkoutPlanExerciseRequest(
                            exerciseId = 4L,
                            orderIndex = 1,
                            sets = 3,
                            repetitions = 10,
                            restSeconds = 60,
                        ),
                    ),
                ),
            ),
        )
        assertEquals(plan, CreateWorkoutPlanUseCase(repository)(2L, request))
    }

    @Test(expected = IllegalArgumentException::class)
    fun createRejectsInvalidDates() {
        runBlocking {
            CreateWorkoutPlanUseCase(FakeRepository())(
                2L,
                CreateWorkoutPlanRequest(
                    startDate = "2026-09-02",
                    endDate = "2026-09-01",
                    days = emptyList(),
                ),
            )
        }
    }
}

private class FakeRepository : WorkoutPlanRepository {
    var plans = emptyList<WorkoutPlan>()
    var plan = WorkoutPlan(1L, 2L, 3L, null, "2026-09-01", null, "ACTIVE", emptyList())

    override suspend fun getMine() = plans
    override suspend fun getMyWeek(weekStart: String) = plans
    override suspend fun create(clientId: Long, request: CreateWorkoutPlanRequest) = plan
    override suspend fun getForClient(clientId: Long) = plans
    override suspend fun get(id: Long) = plan
    override suspend fun update(id: Long, request: UpdateWorkoutPlanRequest) = plan
    override suspend fun addDay(planId: Long, request: WorkoutPlanDayRequest) = plan
    override suspend fun deleteDay(planId: Long, day: Int) = Unit
    override suspend fun addExercise(planId: Long, day: Int, request: WorkoutPlanExerciseRequest) = plan
    override suspend fun updateExercise(id: Long, request: WorkoutPlanExerciseRequest) = plan
    override suspend fun deleteExercise(id: Long) = Unit
    override suspend fun deactivate(id: Long) = Unit
    override suspend fun complete(id: Long) = Unit
}
