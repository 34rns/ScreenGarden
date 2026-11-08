// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(stat: DailyStat): Long

    /** A sync owns usage only; it cannot rewind focus or an applied garden. */
    @Transaction
    suspend fun saveUsage(stat: DailyStat) {
        insertIfAbsent(stat.copy(score = null, gardenApplied = false, plantsGrown = 0))
        updateUsage(
            stat.dayStartUtcMillis,
            stat.screenMinutes,
            stat.pickups,
            stat.unlocks,
            stat.usageIsBounded,
            stat.updatedAtUtcMillis,
            stat.bedtimeMinutes
        )
    }

    @Query(
        "UPDATE daily_stats SET screenMinutes = :screenMinutes, pickups = :pickups, unlocks = :unlocks, " +
            "usageIsBounded = :usageIsBounded, updatedAtUtcMillis = :updatedAtUtcMillis, " +
            "bedtimeMinutes = :bedtimeMinutes, " +
            "score = CASE WHEN gardenApplied = 0 THEN NULL ELSE score END " +
            "WHERE dayStartUtcMillis = :dayStartUtcMillis"
    )
    suspend fun updateUsage(
        dayStartUtcMillis: Long,
        screenMinutes: Int,
        pickups: Int?,
        unlocks: Int?,
        usageIsBounded: Boolean,
        updatedAtUtcMillis: Long,
        bedtimeMinutes: Int? = null
    ): Int

    /** Rejects a calculation if its measured row changed or was already rewarded. */
    @Query(
        "UPDATE daily_stats SET score = :score WHERE dayStartUtcMillis = :dayStartUtcMillis " +
            "AND updatedAtUtcMillis = :expectedUpdatedAtUtcMillis AND gardenApplied = 0"
    )
    suspend fun updateScore(dayStartUtcMillis: Long, score: Int?, expectedUpdatedAtUtcMillis: Long): Int

    @Query(
        "UPDATE daily_stats SET score = :score, plantsGrown = :plantsGrown, gardenApplied = 1 " +
            "WHERE dayStartUtcMillis = :dayStartUtcMillis AND usageIsBounded = 1 " +
            "AND gardenApplied = 0 AND score = :score"
    )
    suspend fun markGardenApplied(dayStartUtcMillis: Long, score: Int, plantsGrown: Int): Int

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

    @Query(
        "SELECT * FROM daily_stats " +
            "WHERE dayStartUtcMillis BETWEEN :fromDayStartUtcMillis AND :toDayStartUtcMillis " +
            "ORDER BY dayStartUtcMillis ASC"
    )
    suspend fun getRange(fromDayStartUtcMillis: Long, toDayStartUtcMillis: Long): List<DailyStat>

    @Update
    suspend fun update(stat: DailyStat): Int

    @Query("UPDATE daily_stats SET isCompassionDay = :enabled WHERE dayStartUtcMillis = :day")
    suspend fun setCompassionDay(day: Long, enabled: Boolean): Int

    @Query("DELETE FROM daily_stats WHERE dayStartUtcMillis = :dayStartUtcMillis")
    suspend fun deleteByDay(dayStartUtcMillis: Long): Int

    @Query("DELETE FROM daily_stats")
    suspend fun deleteAll()
}
