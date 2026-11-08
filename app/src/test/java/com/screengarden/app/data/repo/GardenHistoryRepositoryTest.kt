// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.data.local.entity.DailyStat
import com.screengarden.app.data.local.entity.GardenHistory
import com.screengarden.app.domain.garden.GardenDay
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GardenHistoryRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        ScreenGardenDatabase::class.java
    ).build()
    private val garden = GardenRepositoryImpl(database)
    private val history = GardenHistoryRepository(database.gardenHistoryDao())

    @After fun close() = database.close()

    @Test fun completedGrowthStoresOneActualImmutableFrameInTheSameTransaction() = runBlocking {
        val day = 1_794_000_000_000L
        garden.savePlant(Plant("same-plant", "flower", 44, 42, PlantCondition.WILTING, day - 1))
        database.dailyStatDao().upsert(DailyStat(day, updatedAtUtcMillis = day, score = 80, usageIsBounded = true))
        assertTrue(garden.advanceCompletedDay(GardenDay(score = 80, streakDays = 1, dayStartUtcMillis = day)))
        val captured = history.observe().first().frames.single()
        assertEquals(garden.observePlants().first(), captured.plants)
        assertFalse(garden.advanceCompletedDay(GardenDay(score = 80, streakDays = 1, dayStartUtcMillis = day)))
        garden.savePlant(Plant("same-plant", "flower", 100, 100, plantedAtUtcMillis = day - 1))
        assertEquals(captured, history.observe().first().frames.single())
    }

    @Test fun malformedAndFutureFramesAreReportedWithoutCrashingOrInventingPlants() = runBlocking {
        database.gardenHistoryDao().insertIfAbsent(GardenHistory(1, 2, 80, "broken"))
        database.gardenHistoryDao().insertIfAbsent(GardenHistory(2, 3, 80, "{\"version\":9,\"plants\":[]}"))
        database.gardenHistoryDao().insertIfAbsent(GardenHistory(3, 4, 80, GardenHistoryCodec.encode(emptyList())))
        val result = history.observe().first()
        assertEquals(2, result.unreadableFrames)
        assertEquals(3L, result.frames.single().dayStartUtcMillis)
        assertEquals(emptyList<Plant>(), result.frames.single().plants)
    }

    @Test fun codecRetainsRecoveringPlantsAndNullOrActualFocusTimestamps() {
        val plants = listOf(Plant("a", "fern", 50, 35, PlantCondition.RECOVERING, 1, 2), Plant("b", "cactus"))
        assertEquals(plants, GardenHistoryCodec.decode(GardenHistoryCodec.encode(plants)))
    }
}
