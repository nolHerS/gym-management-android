package com.imanol.gymmanagement.feature.nutrition.data

import com.imanol.gymmanagement.feature.nutrition.domain.NutritionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NutritionDataModule {
    @Binds
    @Singleton
    abstract fun bindNutritionRepository(
        repository: NutritionRepositoryImpl,
    ): NutritionRepository
}
