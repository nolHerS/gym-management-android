package com.imanol.gymmanagement.feature.auth.data

import com.imanol.gymmanagement.feature.auth.domain.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        repository: AuthRepositoryImpl,
    ): AuthRepository
}
