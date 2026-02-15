package com.healthsync.ai.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.healthsync.ai.data.local.db.entity.HealthMetricsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthMetricsDao {
    @Query("SELECT * FROM health_metrics WHERE date = :date")
    suspend fun getByDate(date: String): HealthMetricsEntity?

    @Query("SELECT * FROM health_metrics WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getForDateRange(startDate: String, endDate: String): Flow<List<HealthMetricsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metrics: HealthMetricsEntity)

    @Query("SELECT * FROM health_metrics ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<HealthMetricsEntity>
}
