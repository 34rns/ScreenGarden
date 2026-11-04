// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.data.local.dao.AppUsageDailyDao
import com.screengarden.app.data.local.entity.AppUsageDaily
import kotlinx.coroutines.flow.Flow

/**
 * Room-backed [AppUsageRepository].
 *
 * Implemented in P06 for the daily sync. The composite key
 * (dayStartUtcMillis, packageName) plus REPLACE semantics is what makes the sync
 * idempotent: writing the same day twice updates the same rows instead of adding
 * a second set.
 */
class AppUsageRepositoryImpl(private val dao: AppUsageDailyDao) : AppUsageRepository {

    override fun observeDay(dayStartUtcMillis: Long): Flow<List<AppUsageDaily>> = dao.observeDay(dayStartUtcMillis)

    override fun observeApp(
        packageName: String,
        fromDayStartUtcMillis: Long,
        toDayStartUtcMillis: Long
    ): Flow<List<AppUsageDaily>> = dao.observeApp(packageName, fromDayStartUtcMillis, toDayStartUtcMillis)

    override suspend fun get(dayStartUtcMillis: Long, packageName: String): AppUsageDaily? =
        dao.get(dayStartUtcMillis, packageName)

    override suspend fun save(usage: AppUsageDaily) = dao.upsert(usage)

    override suspend fun saveAll(usage: List<AppUsageDaily>) {
        // One transaction-less bulk insert is enough: a partially written day is
        // repaired by the next sync, which writes the same rows again.
        if (usage.isNotEmpty()) dao.upsertAll(usage)
    }

    override suspend fun deleteDay(dayStartUtcMillis: Long) {
        dao.deleteDay(dayStartUtcMillis)
    }
}
