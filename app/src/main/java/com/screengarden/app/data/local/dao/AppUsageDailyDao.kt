// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.screengarden.app.data.local.entity.AppUsageDaily
import kotlinx.coroutines.flow.Flow

/** Reads and writes per-app daily usage (see [AppUsageDaily]). */
@Dao
interface AppUsageDailyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(usage: AppUsageDaily)

    /** Bulk write used when a day's usage is collected in one pass. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(usage: List<AppUsageDaily>)

    @Query(
        "SELECT * FROM app_usage_daily " +
            "WHERE dayStartUtcMillis = :dayStartUtcMillis AND packageName = :packageName"
    )
    suspend fun get(dayStartUtcMillis: Long, packageName: String): AppUsageDaily?

    @Query(
        "SELECT * FROM app_usage_daily WHERE dayStartUtcMillis = :dayStartUtcMillis " +
            "ORDER BY usageMillis DESC"
    )
    fun observeDay(dayStartUtcMillis: Long): Flow<List<AppUsageDaily>>

    @Query("SELECT * FROM app_usage_daily WHERE dayStartUtcMillis BETWEEN :fromDay AND :toDay")
    fun observeRange(fromDay: Long, toDay: Long): Flow<List<AppUsageDaily>>

    @Query("SELECT * FROM app_usage_daily WHERE dayStartUtcMillis BETWEEN :fromDay AND :toDay")
    suspend fun getRange(fromDay: Long, toDay: Long): List<AppUsageDaily>

    @Query(
        "SELECT * FROM app_usage_daily " +
            "WHERE packageName = :packageName " +
            "AND dayStartUtcMillis BETWEEN :fromDayStartUtcMillis AND :toDayStartUtcMillis " +
            "ORDER BY dayStartUtcMillis ASC"
    )
    fun observeApp(
        packageName: String,
        fromDayStartUtcMillis: Long,
        toDayStartUtcMillis: Long
    ): Flow<List<AppUsageDaily>>

    @Update
    suspend fun update(usage: AppUsageDaily): Int

    @Query(
        "DELETE FROM app_usage_daily " +
            "WHERE dayStartUtcMillis = :dayStartUtcMillis AND packageName = :packageName"
    )
    suspend fun delete(dayStartUtcMillis: Long, packageName: String): Int

    @Query("DELETE FROM app_usage_daily WHERE dayStartUtcMillis = :dayStartUtcMillis")
    suspend fun deleteDay(dayStartUtcMillis: Long): Int
}
