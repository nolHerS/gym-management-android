package com.imanol.gymmanagement.feature.client.data

import com.imanol.gymmanagement.feature.client.domain.ClientRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientDataModule {
    @Binds
    @Singleton
    abstract fun bindClientRepository(
        repository: ClientRepositoryImpl,
    ): ClientRepository
}
