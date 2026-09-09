package com.imanol.gymmanagement.feature.exercise.data

import com.imanol.gymmanagement.feature.exercise.domain.ExerciseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExerciseDataModule {
    @Binds
    @Singleton
    abstract fun bindExerciseRepository(
        repository: ExerciseRepositoryImpl,
    ): ExerciseRepository
}
