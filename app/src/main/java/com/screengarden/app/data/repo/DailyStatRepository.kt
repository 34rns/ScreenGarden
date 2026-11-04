// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.data.local.entity.DailyStat
import kotlinx.coroutines.flow.Flow

/**
 * Day aggregates for Insights and the garden's daily summary.
 *
 * Implemented by [DailyStatRepositoryImpl] over Room since P06, because the daily
 * sync has to persist a day's screen time. The remaining aggregation (the health
 * score) still belongs to a later prompt, which is why no screen reads this yet.
 */
interface DailyStatRepository {

    fun observeDay(dayStartUtcMillis: Long): Flow<DailyStat?>

    fun observeRange(fromDayStartUtcMillis: Long, toDayStartUtcMillis: Long): Flow<List<DailyStat>>

    suspend fun getDay(dayStartUtcMillis: Long): DailyStat?

    suspend fun save(stat: DailyStat)

    suspend fun deleteDay(dayStartUtcMillis: Long)
}
