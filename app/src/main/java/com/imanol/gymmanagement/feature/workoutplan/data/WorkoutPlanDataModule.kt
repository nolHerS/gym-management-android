package com.imanol.gymmanagement.feature.workoutplan.data

import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
abstract class WorkoutPlanDataModule {
    @Binds @Singleton abstract fun bind(repository: WorkoutPlanRepositoryImpl): WorkoutPlanRepository
}
