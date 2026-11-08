// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.insights

import com.screengarden.app.domain.garden.GardenDayRecord
import kotlinx.coroutines.flow.Flow

enum class InsightsPeriod(val dayCount: Int) { DAY(1), WEEK(7), MONTH(30) }
data class AppTime(val dayStartUtcMillis: Long, val packageName: String, val label: String, val usageMillis: Long)
data class InsightsSnapshot(val days: List<GardenDayRecord>, val apps: List<AppTime>)
data class HourlyDay(val dayStartUtcMillis: Long, val usageMillis: List<Long>)

/** Persisted totals and transient recent hour measurements stay distinct. */
interface InsightsStore {
    fun observe(fromDay: Long, toDay: Long): Flow<InsightsSnapshot>
    suspend fun recentHours(fromDay: Long, toDay: Long): List<HourlyDay>
}
