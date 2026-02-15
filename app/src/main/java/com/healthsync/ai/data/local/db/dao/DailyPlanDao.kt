package com.healthsync.ai.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.healthsync.ai.data.local.db.entity.DailyPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyPlanDao {
    @Query("SELECT * FROM daily_plans WHERE date = :date")
    suspend fun getByDate(date: String): DailyPlanEntity?

    @Query("SELECT * FROM daily_plans WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getForDateRange(startDate: String, endDate: String): Flow<List<DailyPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: DailyPlanEntity)
}
