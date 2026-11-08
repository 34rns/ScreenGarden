// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.data.usage.DayUsageResult
import com.screengarden.app.data.usage.UsageRepository
import com.screengarden.app.domain.garden.GardenStore
import com.screengarden.app.domain.insights.AppTime
import com.screengarden.app.domain.insights.HourlyDay
import com.screengarden.app.domain.insights.InsightsSnapshot
import com.screengarden.app.domain.insights.InsightsStore
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** No hourly history is fabricated: Android events expire, while Room's daily totals remain. */
class InsightsRepository(
    private val database: ScreenGardenDatabase,
    private val garden: GardenStore,
    private val usage: UsageRepository,
    private val appLabels: () -> Map<String, String>,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault()
) : InsightsStore {
    override fun observe(fromDay: Long, toDay: Long): Flow<InsightsSnapshot> = flow {
        val labels = appLabels()
        emitAll(
            combine(
                garden.observeSnapshot(fromDay, toDay),
                database.appUsageDailyDao().observeRange(fromDay, toDay)
            ) { snapshot, apps ->
                InsightsSnapshot(
                    snapshot.days,
                    apps.map {
                        AppTime(
                            it.dayStartUtcMillis,
                            it.packageName,
                            labels[it.packageName] ?: it.packageName,
                            it.usageMillis
                        )
                    }
                )
            }
        )
    }

    override suspend fun recentHours(fromDay: Long, toDay: Long): List<HourlyDay> {
        var earliest = LocalDay.startOfDay(nowMillis(), zone)
        repeat(6) { earliest = LocalDay.previousDayStart(earliest, zone) }
        var cursor = maxOf(earliest, fromDay)
        val result = mutableListOf<HourlyDay>()
        while (cursor <= toDay) {
            val read = usage.readDay(cursor, zone)
            if (read is DayUsageResult.MissingPermission) break
            val hours = (read as DayUsageResult.Available).usage.hourlyUsageMillis
            if (hours?.size == 24) result += HourlyDay(cursor, hours)
            cursor = LocalDay.nextDayStart(cursor, zone)
        }
        return result
    }
}
