package com.imanol.gymmanagement.feature.workoutexecution.data

import com.imanol.gymmanagement.feature.workoutexecution.domain.SystemWorkoutExecutionDateProvider
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionDateProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkoutExecutionDataModule {
    @Binds
    @Singleton
    abstract fun bindDateProvider(
        provider: SystemWorkoutExecutionDateProvider,
    ): WorkoutExecutionDateProvider
}
