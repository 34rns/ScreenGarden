// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.sync

import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.data.local.entity.AppUsageDaily
import com.screengarden.app.data.local.entity.DailyStat
import com.screengarden.app.data.repo.AppUsageRepository
import com.screengarden.app.data.repo.DailyStatRepository
import com.screengarden.app.data.usage.AppUsage
import com.screengarden.app.data.usage.DayUsage
import com.screengarden.app.data.usage.DayUsageResult
import com.screengarden.app.data.usage.UnlockCount
import com.screengarden.app.data.usage.UsageRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The idempotency contract of the daily sync (P06).
 *
 * Plain JVM tests over fakes: no Robolectric, no WorkManager, no UsageStatsManager.
 * That is the point of keeping [DailyUsageSync] a plain class - the rules that decide
 * what lands in Room are tested in milliseconds, and the worker is left with nothing
 * but wiring.
 */
class DailyUsageSyncTest {

    @Test
    fun secondRunOverwritesTheSameRowsInsteadOfDuplicating() = runTest {
        val usage = FakeUsageRepository()
        val stats = FakeDailyStatRepository()
        val appUsage = FakeAppUsageRepository()
        val sync = sync(usage, stats, appUsage)
        stats.stats[TODAY] = DailyStat(dayStartUtcMillis = TODAY, updatedAtUtcMillis = NOW)
        stats.stats[YESTERDAY] = DailyStat(dayStartUtcMillis = YESTERDAY, updatedAtUtcMillis = NOW)

        sync.run()
        val afterFirstRun = appUsage.rows.toMap()
        sync.run()

        // Same rows, same values: two apps on each of two days, no duplicates.
        assertEquals(afterFirstRun, appUsage.rows.toMap())
        assertEquals(4, appUsage.rows.size)
        assertEquals(2, stats.stats.size)
        assertEquals(SCREEN_MINUTES, stats.stats.getValue(TODAY).screenMinutes)
    }

    @Test
    fun resyncKeepsFieldsOwnedByOtherFeatures() = runTest {
        val stats = FakeDailyStatRepository()
        // Focus minutes and sessions are written by the focus feature, not the sync.
        stats.stats[YESTERDAY] = DailyStat(
            dayStartUtcMillis = YESTERDAY,
            focusMinutes = 45,
            sessionsCompleted = 2,
            plantsGrown = 1,
            updatedAtUtcMillis = NOW - 1
        )

        sync(FakeUsageRepository(), stats, FakeAppUsageRepository()).run()

        val stored = stats.stats.getValue(YESTERDAY)
        assertEquals(45, stored.focusMinutes)
        assertEquals(2, stored.sessionsCompleted)
        assertEquals(1, stored.plantsGrown)
        assertEquals(SCREEN_MINUTES, stored.screenMinutes)
        assertEquals(NOW, stored.updatedAtUtcMillis)
    }

    @Test
    fun firstRunBackfillsSevenDays() = runTest {
        val usage = FakeUsageRepository()
        val stats = FakeDailyStatRepository()

        val result = sync(usage, stats, FakeAppUsageRepository()).run()

        assertEquals(7, usage.requestedDays.size)
        assertEquals(7, usage.requestedDays.distinct().size)
        assertEquals(7, (result as DailyUsageSyncResult.Synced).days.size)
        assertEquals(7, stats.stats.size)
        assertTrue(usage.requestedDays.contains(TODAY))
        assertTrue(usage.requestedDays.contains(YESTERDAY))
    }

    @Test
    fun laterRunsOnlySyncTodayAndYesterday() = runTest {
        val usage = FakeUsageRepository()
        val stats = FakeDailyStatRepository()
        stats.stats[YESTERDAY] = DailyStat(dayStartUtcMillis = YESTERDAY, updatedAtUtcMillis = NOW - 1)

        sync(usage, stats, FakeAppUsageRepository()).run()

        assertEquals(listOf(TODAY, YESTERDAY), usage.requestedDays)
    }

    @Test
    fun missingUsageAccessReadsAndWritesNothing() = runTest {
        val usage = FakeUsageRepository(granted = false)
        val stats = FakeDailyStatRepository()
        val appUsage = FakeAppUsageRepository()

        val result = sync(usage, stats, appUsage).run()

        assertEquals(DailyUsageSyncResult.MissingUsageAccess, result)
        assertEquals(emptyList<Long>(), usage.requestedDays)
        assertEquals(0, stats.stats.size)
        assertEquals(0, appUsage.rows.size)
    }

    @Test
    fun screenTimeIsStoredAsWholeMinutesRoundedDown() = runTest {
        val stats = FakeDailyStatRepository()
        stats.stats[YESTERDAY] = DailyStat(dayStartUtcMillis = YESTERDAY, updatedAtUtcMillis = NOW)

        sync(FakeUsageRepository(), stats, FakeAppUsageRepository()).run()

        // Two 45s apps are 90s together: stored as 1 minute, never rounded up, so the
        // app cannot claim a minute the user has not used yet.
        assertEquals(1, stats.stats.getValue(YESTERDAY).screenMinutes)
    }

    @Test
    fun perAppRowsAreKeyedByTheDayTheyBelongTo() = runTest {
        val appUsage = FakeAppUsageRepository()

        sync(FakeUsageRepository(), FakeDailyStatRepository(), appUsage).run()

        val yesterdayRows = appUsage.rows.values.filter { it.dayStartUtcMillis == YESTERDAY }
        assertEquals(2, yesterdayRows.size)
        assertEquals(45_000L, yesterdayRows.first().usageMillis)
        assertTrue(yesterdayRows.all { it.updatedAtUtcMillis == NOW })
    }

    private fun sync(
        usageRepository: UsageRepository,
        dailyStatRepository: DailyStatRepository,
        appUsageRepository: AppUsageRepository
    ): DailyUsageSync = DailyUsageSync(
        usageRepository = usageRepository,
        dailyStatRepository = dailyStatRepository,
        appUsageRepository = appUsageRepository,
        nowMillis = { NOW },
        zoneId = ZONE
    )

    /** Serves the same two-app day for whatever day is asked for. */
    private class FakeUsageRepository(var granted: Boolean = true) : UsageRepository {

        val requestedDays = mutableListOf<Long>()

        override suspend fun isUsageAccessGranted(): Boolean = granted

        override suspend fun readDay(dayStartUtcMillis: Long, zoneId: ZoneId): DayUsageResult {
            requestedDays += dayStartUtcMillis
            if (!granted) return DayUsageResult.MissingPermission
            return DayUsageResult.Available(
                DayUsage(
                    dayStartUtcMillis = dayStartUtcMillis,
                    totalScreenTimeMillis = SCREEN_MILLIS,
                    pickups = 4,
                    unlocks = UnlockCount.Supported(1),
                    apps = listOf(
                        AppUsage(PKG_A, APP_MILLIS, launchCount = 2, lastUsedUtcMillis = dayStartUtcMillis),
                        AppUsage(PKG_B, APP_MILLIS, launchCount = 1, lastUsedUtcMillis = dayStartUtcMillis)
                    )
                )
            )
        }
    }

    private class FakeDailyStatRepository : DailyStatRepository {

        val stats = linkedMapOf<Long, DailyStat>()

        override fun observeDay(dayStartUtcMillis: Long): Flow<DailyStat?> = flowOf(stats[dayStartUtcMillis])

        override fun observeRange(fromDayStartUtcMillis: Long, toDayStartUtcMillis: Long): Flow<List<DailyStat>> =
            flowOf(stats.filterKeys { it in fromDayStartUtcMillis..toDayStartUtcMillis }.values.toList())

        override suspend fun getDay(dayStartUtcMillis: Long): DailyStat? = stats[dayStartUtcMillis]

        override suspend fun save(stat: DailyStat) {
            stats[stat.dayStartUtcMillis] = stat
        }

        override suspend fun deleteDay(dayStartUtcMillis: Long) {
            stats.remove(dayStartUtcMillis)
        }
    }

    private class FakeAppUsageRepository : AppUsageRepository {

        /** Keyed exactly like the table: a re-sync replaces, it does not append. */
        val rows = linkedMapOf<Pair<Long, String>, AppUsageDaily>()

        override fun observeDay(dayStartUtcMillis: Long): Flow<List<AppUsageDaily>> =
            flowOf(rowsFor(dayStartUtcMillis, packageName = null))

        override fun observeApp(
            packageName: String,
            fromDayStartUtcMillis: Long,
            toDayStartUtcMillis: Long
        ): Flow<List<AppUsageDaily>> = flowOf(
            rows.values.filter {
                it.packageName == packageName && it.dayStartUtcMillis in fromDayStartUtcMillis..toDayStartUtcMillis
            }
        )

        override suspend fun get(dayStartUtcMillis: Long, packageName: String): AppUsageDaily? =
            rows[dayStartUtcMillis to packageName]

        override suspend fun save(usage: AppUsageDaily) {
            rows[usage.dayStartUtcMillis to usage.packageName] = usage
        }

        override suspend fun saveAll(usage: List<AppUsageDaily>) {
            usage.forEach { rows[it.dayStartUtcMillis to it.packageName] = it }
        }

        override suspend fun deleteDay(dayStartUtcMillis: Long) {
            rows.keys.removeAll { it.first == dayStartUtcMillis }
        }

        private fun rowsFor(dayStartUtcMillis: Long, packageName: String?): List<AppUsageDaily> = rows.values.filter {
            it.dayStartUtcMillis == dayStartUtcMillis && (packageName == null || it.packageName == packageName)
        }
    }

    private companion object {
        val ZONE: ZoneId = ZoneId.of("Europe/Berlin")

        /** A fixed afternoon, so the sync's "today" never depends on the machine. */
        val NOW: Long = LocalDay.startOfDay(LocalDate.of(2026, 11, 3), ZONE) + 14 * 60 * 60 * 1000L
        val TODAY: Long = LocalDay.startOfDay(NOW, ZONE)
        val YESTERDAY: Long = LocalDay.previousDayStart(TODAY, ZONE)

        const val APP_MILLIS = 45_000L
        const val SCREEN_MILLIS = APP_MILLIS * 2
        const val SCREEN_MINUTES = 1

        const val PKG_A = "com.example.a"
        const val PKG_B = "com.example.b"
    }
}
