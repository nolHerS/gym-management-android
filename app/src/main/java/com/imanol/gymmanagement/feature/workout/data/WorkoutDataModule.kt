package com.imanol.gymmanagement.feature.workout.data

import com.imanol.gymmanagement.feature.workout.domain.WorkoutRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkoutDataModule {
    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(
        repository: WorkoutRepositoryImpl,
    ): WorkoutRepository
}
