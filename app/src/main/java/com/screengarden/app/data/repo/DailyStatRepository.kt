// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.data.local.entity.DailyStat
import kotlinx.coroutines.flow.Flow

/**
 * Day aggregates for Insights and the garden's daily summary.
 *
 * INTERFACE ONLY for now (Task 1.1): the implementation lands with the
 * aggregation use case, so nothing can accidentally start reading half-built
 * data through it.
 */
interface DailyStatRepository {

    fun observeDay(dayStartUtcMillis: Long): Flow<DailyStat?>

    fun observeRange(fromDayStartUtcMillis: Long, toDayStartUtcMillis: Long): Flow<List<DailyStat>>

    suspend fun getDay(dayStartUtcMillis: Long): DailyStat?

    suspend fun save(stat: DailyStat)

    suspend fun deleteDay(dayStartUtcMillis: Long)
}
