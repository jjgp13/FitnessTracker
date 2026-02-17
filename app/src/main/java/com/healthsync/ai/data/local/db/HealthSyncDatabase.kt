package com.healthsync.ai.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.healthsync.ai.data.local.db.dao.DailyPlanDao
import com.healthsync.ai.data.local.db.dao.HealthMetricsDao
import com.healthsync.ai.data.local.db.dao.UserProfileDao
import com.healthsync.ai.data.local.db.entity.DailyPlanEntity
import com.healthsync.ai.data.local.db.entity.HealthMetricsEntity
import com.healthsync.ai.data.local.db.entity.UserProfileEntity

@Database(
    entities = [
        HealthMetricsEntity::class,
        DailyPlanEntity::class,
        UserProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HealthSyncDatabase : RoomDatabase() {
    abstract fun healthMetricsDao(): HealthMetricsDao
    abstract fun dailyPlanDao(): DailyPlanDao
    abstract fun userProfileDao(): UserProfileDao
}
