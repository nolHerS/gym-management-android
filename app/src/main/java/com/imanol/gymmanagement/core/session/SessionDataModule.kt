package com.imanol.gymmanagement.core.session

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionDataModule {
    @Binds
    @Singleton
    abstract fun bindSessionCipher(cipher: AndroidSessionCipher): SessionCipher
}
