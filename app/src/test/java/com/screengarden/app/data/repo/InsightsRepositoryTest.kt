// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.data.local.entity.AppUsageDaily
import com.screengarden.app.data.local.entity.DailyStat
import com.screengarden.app.data.usage.DayUsage
import com.screengarden.app.data.usage.DayUsageResult
import com.screengarden.app.data.usage.UnlockCount
import com.screengarden.app.data.usage.UsageRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InsightsRepositoryTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val day = LocalDay.startOfDay(LocalDate.of(2026, 3, 30), zone)
    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        ScreenGardenDatabase::class.java
    ).build()
    private val queries = mutableListOf<Long>()
    private var granted = true
    private val usage = object : UsageRepository {
        override suspend fun isUsageAccessGranted() = granted
        override suspend fun readDay(dayStartUtcMillis: Long, zoneId: ZoneId): DayUsageResult {
            queries += dayStartUtcMillis
            if (!granted) return DayUsageResult.MissingPermission
            return DayUsageResult.Available(
                DayUsage(
                    dayStartUtcMillis,
                    0,
                    0,
                    UnlockCount.Supported(0),
                    emptyList(),
                    usageIsBounded = true,
                    hourlyUsageMillis = List(24) { 0L }
                )
            )
        }
    }
    private fun repository() = InsightsRepository(
        database,
        GardenRepositoryImpl(database),
        usage,
        { mapOf("reader" to "Reading") },
        { day + 60_000 },
        zone
    )

    @After fun close() {
        database.close()
    }

    @Test fun savedRestAndAppRowsRemainVisibleWithLabels() = runBlocking {
        database.dailyStatDao().upsert(
            DailyStat(
                day,
                screenMinutes = 60,
                isCompassionDay = true,
                updatedAtUtcMillis = day
            )
        )
        database.appUsageDailyDao().upsert(AppUsageDaily(day, "reader", 3_600_000, updatedAtUtcMillis = day))
        val snapshot = repository().observe(day, day).first()
        assertEquals(true, snapshot.days.single().isCompassionDay)
        assertEquals("Reading", snapshot.apps.single().label)
        assertEquals(3_600_000L, snapshot.apps.single().usageMillis)
    }

    @Test fun eventReadsAreCappedAtSevenLocalDaysAcrossDst() = runBlocking {
        val rows = repository().recentHours(LocalDay.startOfDay(LocalDate.of(2026, 3, 1), zone), day)
        assertEquals(7, queries.size)
        assertEquals(7, rows.size)
        assertEquals(LocalDay.startOfDay(LocalDate.of(2026, 3, 24), zone), queries.first())
        assertEquals(day, queries.last())
    }

    @Test fun revokedAccessStopsEventQueriesWithoutInventedRows() = runBlocking {
        granted = false
        assertEquals(emptyList<Any>(), repository().recentHours(LocalDay.previousDayStart(day, zone), day))
        assertEquals(1, queries.size)
    }
}
