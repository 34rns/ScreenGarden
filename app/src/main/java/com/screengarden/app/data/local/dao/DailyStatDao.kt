// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.screengarden.app.data.local.entity.DailyStat
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes the one-row-per-day aggregate (see [DailyStat]).
 *
 * Day parameters are always `dayStartUtcMillis` produced by
 * [com.screengarden.app.core.time.LocalDay], never a raw timestamp.
 */
@Dao
interface DailyStatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: DailyStat)

    @Query("SELECT * FROM daily_stats WHERE dayStartUtcMillis = :dayStartUtcMillis")
    suspend fun getByDay(dayStartUtcMillis: Long): DailyStat?

    @Query("SELECT * FROM daily_stats WHERE dayStartUtcMillis = :dayStartUtcMillis")
    fun observeDay(dayStartUtcMillis: Long): Flow<DailyStat?>

    @Query(
        "SELECT * FROM daily_stats " +
            "WHERE dayStartUtcMillis BETWEEN :fromDayStartUtcMillis AND :toDayStartUtcMillis " +
            "ORDER BY dayStartUtcMillis ASC"
    )
    fun observeRange(fromDayStartUtcMillis: Long, toDayStartUtcMillis: Long): Flow<List<DailyStat>>

    @Update
    suspend fun update(stat: DailyStat): Int

    @Query("DELETE FROM daily_stats WHERE dayStartUtcMillis = :dayStartUtcMillis")
    suspend fun deleteByDay(dayStartUtcMillis: Long): Int

    @Query("DELETE FROM daily_stats")
    suspend fun deleteAll()
}
