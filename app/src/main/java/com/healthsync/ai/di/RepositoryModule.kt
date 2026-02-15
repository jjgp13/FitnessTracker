package com.healthsync.ai.di

import com.healthsync.ai.data.repository.AuthRepositoryImpl
import com.healthsync.ai.data.repository.UserProfileRepositoryImpl
import com.healthsync.ai.domain.repository.AuthRepository
import com.healthsync.ai.domain.repository.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository
}
