package com.healthsync.ai.di

import android.content.Context
import androidx.room.Room
import com.healthsync.ai.data.local.db.HealthSyncDatabase
import com.healthsync.ai.data.local.db.dao.DailyPlanDao
import com.healthsync.ai.data.local.db.dao.HealthMetricsDao
import com.healthsync.ai.data.local.db.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HealthSyncDatabase {
        return Room.databaseBuilder(
            context,
            HealthSyncDatabase::class.java,
            "healthsync_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideHealthMetricsDao(db: HealthSyncDatabase): HealthMetricsDao = db.healthMetricsDao()

    @Provides
    fun provideDailyPlanDao(db: HealthSyncDatabase): DailyPlanDao = db.dailyPlanDao()

    @Provides
    fun provideUserProfileDao(db: HealthSyncDatabase): UserProfileDao = db.userProfileDao()
}
