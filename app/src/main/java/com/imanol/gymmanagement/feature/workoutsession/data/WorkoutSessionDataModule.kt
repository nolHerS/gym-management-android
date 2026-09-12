package com.imanol.gymmanagement.feature.workoutsession.data

import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkoutSessionDataModule {
    @Binds
    @Singleton
    abstract fun bindRepository(repository: WorkoutSessionRepositoryImpl): WorkoutSessionRepository
}
