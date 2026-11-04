// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.data.local.dao.DailyStatDao
import com.screengarden.app.data.local.entity.DailyStat
import kotlinx.coroutines.flow.Flow

/**
 * Room-backed [DailyStatRepository].
 *
 * This is the first of the six repositories to get an implementation (P06), because
 * the daily sync has to persist a day's screen time somewhere. It stays a thin
 * adapter: no mapping, no arithmetic, no decisions - the aggregate is computed by
 * [com.screengarden.app.data.sync.DailyUsageSync] and this class only stores it.
 *
 * [save] is an upsert (REPLACE on the day key), which is what makes a repeated sync
 * of the same day harmless: the row is overwritten with the same values instead of
 * accumulating duplicates.
 */
class DailyStatRepositoryImpl(private val dao: DailyStatDao) : DailyStatRepository {

    override fun observeDay(dayStartUtcMillis: Long): Flow<DailyStat?> = dao.observeDay(dayStartUtcMillis)

    override fun observeRange(fromDayStartUtcMillis: Long, toDayStartUtcMillis: Long): Flow<List<DailyStat>> =
        dao.observeRange(fromDayStartUtcMillis, toDayStartUtcMillis)

    override suspend fun getDay(dayStartUtcMillis: Long): DailyStat? = dao.getByDay(dayStartUtcMillis)

    override suspend fun save(stat: DailyStat) = dao.upsert(stat)

    override suspend fun deleteDay(dayStartUtcMillis: Long) {
        // The row count is irrelevant to callers; the interface reports nothing.
        dao.deleteByDay(dayStartUtcMillis)
    }
}
