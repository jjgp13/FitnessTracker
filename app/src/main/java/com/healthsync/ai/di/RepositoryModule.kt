package com.healthsync.ai.di

import com.healthsync.ai.data.repository.AuthRepositoryImpl
import com.healthsync.ai.data.repository.CalendarRepositoryImpl
import com.healthsync.ai.data.repository.HealthMetricsRepositoryImpl
import com.healthsync.ai.data.repository.UserProfileRepositoryImpl
import com.healthsync.ai.domain.repository.AuthRepository
import com.healthsync.ai.domain.repository.CalendarRepository
import com.healthsync.ai.domain.repository.HealthMetricsRepository
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

    @Binds
    @Singleton
    abstract fun bindHealthMetricsRepository(impl: HealthMetricsRepositoryImpl): HealthMetricsRepository

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(impl: CalendarRepositoryImpl): CalendarRepository
}
