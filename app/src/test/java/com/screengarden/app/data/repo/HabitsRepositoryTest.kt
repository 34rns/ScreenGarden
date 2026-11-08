// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.data.local.entity.Badge
import com.screengarden.app.data.local.entity.DailyStat
import com.screengarden.app.domain.garden.GardenDay
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.domain.habits.CompassionResult
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HabitsRepositoryTest {
    private lateinit var database: ScreenGardenDatabase
    private lateinit var repository: HabitsRepository
    private val zone = ZoneId.of("Europe/Berlin")
    private val today = LocalDate.of(2026, 11, 8)
    private val now = LocalDay.startOfDay(today, zone) + 3_600_000

    @Before fun setup() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext<Context>(),
                ScreenGardenDatabase::class.java
            )
                .allowMainThreadQueries().build()
        repository = HabitsRepository(database, { now }, zone)
    }

    @After fun close() {
        database.close()
    }

    @Test fun concurrentMarksCannotExceedTheMonthlyAllowance() = runTest {
        val results = (0L..7L).map { offset ->
            async { repository.setCompassionDay(today.minusDays(offset), true, 3) }
        }.awaitAll()
        assertEquals(3, results.count { it == CompassionResult.SAVED })
        assertEquals(3, repository.observe().first().days.count { it.isCompassionDay })
    }

    @Test fun monthsAreIndependentAndRemovingAMarkReturnsItsAllowance() = runTest {
        val october = LocalDate.of(2026, 10, 31)
        assertEquals(CompassionResult.SAVED, repository.setCompassionDay(october, true, 1))
        assertEquals(CompassionResult.SAVED, repository.setCompassionDay(today, true, 1))
        assertEquals(CompassionResult.LIMIT_REACHED, repository.setCompassionDay(today.minusDays(1), true, 1))
        assertEquals(CompassionResult.SAVED, repository.setCompassionDay(today, false, 1))
        assertEquals(CompassionResult.SAVED, repository.setCompassionDay(today.minusDays(1), true, 1))
    }

    @Test fun futureDatesAndZeroAllowancesDoNotCreateRows() = runTest {
        assertEquals(CompassionResult.FUTURE_DATE, repository.setCompassionDay(today.plusDays(1), true, 3))
        assertEquals(CompassionResult.LIMIT_REACHED, repository.setCompassionDay(today, true, 0))
        assertTrue(repository.observe().first().days.isEmpty())
    }

    @Test fun resyncKeepsCompassionAndAppliedFocusGrowth() = runTest {
        val key = LocalDay.startOfDay(today, zone)
        database.dailyStatDao().upsert(
            DailyStat(
                key,
                focusMinutes = 25,
                sessionsCompleted = 1,
                plantsGrown = 2,
                score = 80,
                gardenApplied = true,
                updatedAtUtcMillis = now
            )
        )
        repository.setCompassionDay(today, true, 3)
        database.dailyStatDao().saveUsage(DailyStat(key, screenMinutes = 120, updatedAtUtcMillis = now + 1))
        val row = database.dailyStatDao().getByDay(key)!!
        assertTrue(row.isCompassionDay)
        assertTrue(row.gardenApplied)
        assertEquals(25, row.focusMinutes)
        assertEquals(80, row.score)
        assertEquals(2, row.plantsGrown)
    }

    @Test fun restMarkersRejectDailyGrowthEvenIfTheCallerHasAStaleScore() = runTest {
        val key = LocalDay.startOfDay(today.minusDays(1), zone)
        database.dailyStatDao().upsert(DailyStat(key, score = 100, usageIsBounded = true, updatedAtUtcMillis = now))
        repository.setCompassionDay(today.minusDays(1), true, 3)
        assertFalse(GardenRepositoryImpl(database).advanceCompletedDay(GardenDay(dayStartUtcMillis = key, score = 100)))
        assertEquals(0, database.plantDao().getAll().size)
    }

    @Test fun bloomAndRecoveryAreRecordedAtomicallyAndNeverRevokeOrRetimestamp() = runTest {
        val key = LocalDay.startOfDay(today.minusDays(1), zone)
        val garden = GardenRepositoryImpl(database)
        garden.savePlant(Plant("fern", "fern", 44, 56, PlantCondition.WILTING, key))
        database.dailyStatDao().upsert(DailyStat(key, score = 100, usageIsBounded = true, updatedAtUtcMillis = now))
        assertTrue(garden.advanceCompletedDay(GardenDay(dayStartUtcMillis = key, score = 100)))
        val original = database.badgeDao().getAll()
        assertEquals(setOf("first-bloom", "first-recovery"), original.map { it.badgeKey }.toSet())
        garden.savePlant(garden.getPlant("fern")!!.copy(health = 10, condition = PlantCondition.WILTING))
        repository.evaluateBadges(100)
        val badges = BadgeRepositoryImpl(database.badgeDao())
        original.forEach { badges.save(Badge(it.badgeKey, now + 99_999)) }
        assertEquals(original.toSet(), database.badgeDao().getAll().toSet())
    }
}
