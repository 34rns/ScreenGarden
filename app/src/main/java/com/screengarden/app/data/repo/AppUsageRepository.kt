// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.data.local.entity.AppUsageDaily
import kotlinx.coroutines.flow.Flow

/**
 * Per-app screen time.
 *
 * INTERFACE ONLY for now (Task 1.1). The implementation will sit on
 * [com.screengarden.app.data.local.dao.AppUsageDailyDao] and be fed by the
 * usage-stats collector in Phase 2.
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
