// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.sync

import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.data.local.entity.AppUsageDaily
import com.screengarden.app.data.local.entity.DailyStat
import com.screengarden.app.data.repo.AppUsageRepository
import com.screengarden.app.data.repo.DailyStatRepository
import com.screengarden.app.data.usage.DayUsage
import com.screengarden.app.data.usage.DayUsageResult
import com.screengarden.app.data.usage.UnlockCount
import com.screengarden.app.data.usage.UsageRepository
import java.time.ZoneId

/**
 * The daily sync (P06): reads screen time from the OS and stores it in Room.
 *
 * This is the only place where the read-only usage layer meets the database, and it
 * is deliberately a plain Kotlin class over the three repository interfaces, so the
 * whole thing runs in a JVM test with fakes instead of needing a device, a worker or
 * a real UsageStatsManager.
 *
 * IDEMPOTENCY. Running this any number of times for the same day must leave the same
 * rows behind. That holds because:
 * - `app_usage_daily` is keyed by (dayStartUtcMillis, packageName) and written with
 *   REPLACE, so a second pass overwrites its own rows (no duplicates, no doubling);
 * - a day's screen time only ever grows within that day, so a later read of the same
 *   day is a superset of the previous one, and overwriting with it is correct;
 * - the `daily_stats` row is upserted by day key, and only the fields this sync owns
 *   ([DailyStat.screenMinutes], [DailyStat.updatedAtUtcMillis]) are replaced, so
 *   focus minutes and sessions written by other features survive a re-sync;
 * - nothing is deleted: a row for an app that has since been uninstalled keeps the
 *   time it really had that day, which is history, not garbage.
 *
 * READS. Today and yesterday are always synced. When yesterday has no stored row -
 * the first run ever, or the first run after the app was not used for a while - the
 * rest of the week is backfilled too ([MAX_DAYS]). Android does not keep usage
 * events forever: `UsageStatsManager` retains roughly a week of events (the exact
 * cutoff is not part of the SDK contract and varies by OEM and by whether the device
 * has been charging), so anything older than about 7 days is usually unavailable and
 * asking for more would only produce empty days. That cap is why the backfill is
 * bounded rather than "everything since install".
 *
 * Missing access is reported, not thrown (D46), and it stops the pass immediately:
 * every remaining day would return the same answer.
 */
class DailyUsageSync(
    private val usageRepository: UsageRepository,
    private val dailyStatRepository: DailyStatRepository,
    private val appUsageRepository: AppUsageRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    /** Reads the days that need it and writes them; safe to call repeatedly. */
    suspend fun run(): DailyUsageSyncResult {
        if (!usageRepository.isUsageAccessGranted()) return DailyUsageSyncResult.MissingUsageAccess

        val now = nowMillis()
        val days = daysToSync(LocalDay.startOfDay(now, zoneId))
        val synced = ArrayList<Long>(days.size)
        for (dayStartUtcMillis in days) {
            when (val result = usageRepository.readDay(dayStartUtcMillis, zoneId)) {
                // Access can be revoked between the check above and a read; the rows
                // already written stay valid, so the pass simply ends here.
                DayUsageResult.MissingPermission -> return DailyUsageSyncResult.MissingUsageAccess
                is DayUsageResult.Available -> {
                    persist(dayStartUtcMillis, result.usage, now)
                    synced += dayStartUtcMillis
                }
            }
        }
        return DailyUsageSyncResult.Synced(synced)
    }

    /**
     * Today, yesterday, and on a gap the rest of the backfill window.
     *
     * Yesterday's stored row is the marker for "this app has synced before": today's
     * row alone would not do, because a sync that ran at 00:01 has not written
     * yesterday yet. Using an existing row instead of a settings flag means the
     * backfill repeats (cheaply) after a gap, which is the behaviour we want - a
     * week away from the app heals itself on the next launch.
     */
    private suspend fun daysToSync(todayStartUtcMillis: Long): List<Long> {
        val yesterdayStartUtcMillis = LocalDay.previousDayStart(todayStartUtcMillis, zoneId)
        val days = mutableListOf(todayStartUtcMillis, yesterdayStartUtcMillis)
        if (dailyStatRepository.getDay(yesterdayStartUtcMillis) != null) return days

        var dayStartUtcMillis = yesterdayStartUtcMillis
        repeat(MAX_DAYS - days.size) {
            dayStartUtcMillis = LocalDay.previousDayStart(dayStartUtcMillis, zoneId)
            days += dayStartUtcMillis
        }
        return days
    }

    /** Writes one day of per-app rows plus the day aggregate. */
    private suspend fun persist(dayStartUtcMillis: Long, usage: DayUsage, nowMillis: Long) {
        val rows = usage.apps.map { app ->
            AppUsageDaily(
                dayStartUtcMillis = dayStartUtcMillis,
                packageName = app.packageName,
                usageMillis = app.usageMillis,
                launchCount = app.launchCount,
                updatedAtUtcMillis = nowMillis
            )
        }
        appUsageRepository.saveAll(rows)

        // The aggregate belongs to several features (focus, plants, score), so it is
        // read back and only this sync's own fields are replaced. A missing row means
        // the other fields are still their defaults, which the entity already is.
        val screenMinutes = (usage.totalScreenTimeMillis / MILLIS_PER_MINUTE).toInt()
        dailyStatRepository.saveUsage(
            DailyStat(
                dayStartUtcMillis = dayStartUtcMillis,
                screenMinutes = screenMinutes,
                pickups = if (usage.usageIsBounded) usage.pickups else null,
                unlocks = (usage.unlocks as? UnlockCount.Supported)?.count,
                usageIsBounded = usage.usageIsBounded,
                bedtimeMinutes = if (usage.usageIsBounded) {
                    usage.bedtimeUsageMillis?.div(MILLIS_PER_MINUTE)?.coerceIn(0L, Int.MAX_VALUE.toLong())?.toInt()
                } else {
                    null
                },
                updatedAtUtcMillis = nowMillis
            )
        )
    }

    private companion object {
        /** Today plus the six days before it: the useful life of Android's event log. */
        const val MAX_DAYS = 7

        /** Screen time is stored in whole minutes (SECTION 15), rounded down. */
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
