// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.data.local.entity.AppUsageDaily
import kotlinx.coroutines.flow.Flow

/**
 * Per-app screen time.
 *
 * Implemented by [AppUsageRepositoryImpl] over
 * [com.screengarden.app.data.local.dao.AppUsageDailyDao] since P06. Writes come
 * from the daily sync; the first reader is the Insights screen (P19).
 */
interface AppUsageRepository {

    fun observeDay(dayStartUtcMillis: Long): Flow<List<AppUsageDaily>>

    fun observeApp(
        packageName: String,
        fromDayStartUtcMillis: Long,
        toDayStartUtcMillis: Long
    ): Flow<List<AppUsageDaily>>

    suspend fun get(dayStartUtcMillis: Long, packageName: String): AppUsageDaily?

    suspend fun save(usage: AppUsageDaily)

    suspend fun saveAll(usage: List<AppUsageDaily>)

    suspend fun deleteDay(dayStartUtcMillis: Long)
}
