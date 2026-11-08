// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.data.local.entity.DailyStat
import com.screengarden.app.domain.garden.GardenDay
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.domain.garden.PlantStage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GardenRepositoryImplTest {

    private lateinit var database: ScreenGardenDatabase
    private lateinit var repository: GardenRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ScreenGardenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GardenRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun snapshotMapsPlantConditionGrowthAndDayUnknowns() = runTest {
        repository.savePlant(plant().copy(growthPoints = 47, health = 45, condition = PlantCondition.RECOVERING))
        database.dailyStatDao().upsert(stat().copy(focusMinutes = 25, sessionsCompleted = 1))
        database.dailyStatDao().upsert(stat(DAY_TWO).copy(score = 81, pickups = 7, unlocks = 2))

        val snapshot = repository.observeSnapshot(DAY_ONE, DAY_ONE).first()

        assertEquals(1, snapshot.plants.size)
        assertEquals(47, snapshot.plants.single().growthPoints)
        assertEquals(PlantStage.BLOOM, snapshot.plants.single().stage)
        assertEquals(PlantCondition.RECOVERING, snapshot.plants.single().condition)
        assertEquals(1, snapshot.days.size)
        val day = snapshot.days.single()
        assertNull(day.score)
        assertNull(day.pickups)
        assertNull(day.unlocks)
        assertFalse(day.usageIsBounded)
        assertFalse(day.gardenApplied)
        assertEquals(25, day.focusMinutes)
        assertEquals(1, day.sessionsCompleted)
        assertEquals(DAY_ONE, day.updatedAtUtcMillis)
    }

    @Test
    fun savingDomainPlantKeepsSizeConditionAndActualFocusTimestamp() = runTest {
        repository.savePlant(
            plant().copy(
                growthPoints = 52,
                health = 30,
                condition = PlantCondition.WILTING,
                lastFocusAtUtcMillis = DAY_ONE + 1_000L
            )
        )

        val stored = requireNotNull(database.plantDao().getById("plant-1"))
        assertEquals(3, stored.stage)
        assertEquals(52, stored.growthPoints)
        assertEquals("WILTING", stored.condition)
        assertEquals(DAY_ONE + 1_000L, stored.lastFocusAtUtcMillis)
        assertEquals(PlantStage.BLOOM, repository.getPlant("plant-1")?.stage)
    }

    @Test
    fun scoreWriteCannotCreateUnknownDayOrOverwriteNewerUsage() = runTest {
        assertFalse(repository.recordScore(DAY_ONE, 90, DAY_ONE))
        assertNull(database.dailyStatDao().getByDay(DAY_ONE))
        database.dailyStatDao().upsert(stat().copy(screenMinutes = 90, pickups = 3, usageIsBounded = true))
        assertTrue(repository.recordScore(DAY_ONE, 90, DAY_ONE))

        DailyStatRepositoryImpl(database.dailyStatDao()).saveUsage(
            stat().copy(screenMinutes = 180, pickups = 5, usageIsBounded = true, updatedAtUtcMillis = DAY_TWO)
        )

        assertFalse(repository.recordScore(DAY_ONE, 90, DAY_ONE))
        val stored = requireNotNull(database.dailyStatDao().getByDay(DAY_ONE))
        assertNull(stored.score)
        assertEquals(180, stored.screenMinutes)
        assertEquals(5, stored.pickups)
        assertEquals(DAY_TWO, stored.updatedAtUtcMillis)
    }

    @Test
    fun unboundedMissingAndChangedScoresCannotRewardPlants() = runTest {
        repository.savePlant(plant())
        assertFalse(repository.advanceCompletedDay(day(score = 100)))
        database.dailyStatDao().upsert(stat().copy(score = 100))
        assertFalse(repository.advanceCompletedDay(day(score = 100)))
        database.dailyStatDao().upsert(stat().copy(usageIsBounded = true))
        assertFalse(repository.advanceCompletedDay(day(score = null)))
        assertFalse(repository.advanceCompletedDay(day(score = 100)))
        database.dailyStatDao().upsert(stat().copy(score = 80, usageIsBounded = true))
        assertFalse(repository.advanceCompletedDay(day(score = 100)))

        assertEquals(0, repository.getPlant("plant-1")?.growthPoints)
        assertEquals(1, database.plantDao().count())
        assertFalse(requireNotNull(database.dailyStatDao().getByDay(DAY_ONE)).gardenApplied)
    }

    @Test
    fun completedDayRewardsOnceAcrossRetriesAndRepositoryInstances() = runTest {
        repository.savePlant(plant().copy(growthPoints = 8))
        database.dailyStatDao().upsert(stat().copy(score = 100, screenMinutes = 90, usageIsBounded = true))
        val otherRepository = GardenRepositoryImpl(database)
        val completedDay = day(score = 100).copy(focusSessionsCompleted = 1, lastFocusAtUtcMillis = DAY_ONE + 1_000L)

        val results = listOf(
            async { repository.advanceCompletedDay(completedDay) },
            async { otherRepository.advanceCompletedDay(completedDay) }
        ).awaitAll()

        assertEquals(1, results.count { it })
        assertFalse(repository.advanceCompletedDay(completedDay))
        assertEquals(2, database.plantDao().count())
        assertEquals(15, repository.getPlant("plant-1")?.growthPoints)
        assertEquals(74, repository.getPlant("plant-1")?.health)
        assertEquals(DAY_ONE + 1_000L, repository.getPlant("plant-1")?.lastFocusAtUtcMillis)
        val newSeed = repository.observePlants().first().single { it.id != "plant-1" }
        assertEquals(0, newSeed.growthPoints)
        assertNull(newSeed.lastFocusAtUtcMillis)
        val storedDay = requireNotNull(database.dailyStatDao().getByDay(DAY_ONE))
        assertTrue(storedDay.gardenApplied)
        assertEquals(1, storedDay.plantsGrown)
        assertEquals(90, storedDay.screenMinutes)
        assertEquals(DAY_ONE, storedDay.updatedAtUtcMillis)
    }

    @Test
    fun usageResyncCannotClearTheAppliedMarkerOrRepeatRewards() = runTest {
        repository.savePlant(plant())
        database.dailyStatDao().upsert(stat().copy(score = 100, usageIsBounded = true))
        assertTrue(repository.advanceCompletedDay(day(score = 100)))

        DailyStatRepositoryImpl(database.dailyStatDao()).saveUsage(
            stat().copy(screenMinutes = 180, usageIsBounded = true, updatedAtUtcMillis = DAY_TWO)
        )

        assertFalse(repository.recordScore(DAY_ONE, 100, DAY_TWO))
        assertFalse(repository.advanceCompletedDay(day(score = 100)))
        assertEquals(5, repository.getPlant("plant-1")?.growthPoints)
        assertEquals(2, database.plantDao().count())
        assertTrue(requireNotNull(database.dailyStatDao().getByDay(DAY_ONE)).gardenApplied)
    }

    @Test
    fun failedPlantWriteRollsBackGrowthAndAppliedMarker() = runTest {
        repository.savePlant(plant())
        database.dailyStatDao().upsert(stat().copy(score = 100, usageIsBounded = true))
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_new_seed BEFORE INSERT ON plants WHEN NEW.plantId != 'plant-1' " +
                "BEGIN SELECT RAISE(ABORT, 'Test insertion failure'); END"
        )

        var failed = false
        try {
            repository.advanceCompletedDay(day(score = 100))
        } catch (_: SQLiteException) {
            failed = true
        }

        assertTrue(failed)
        assertEquals(0, repository.getPlant("plant-1")?.growthPoints)
        assertEquals(60, repository.getPlant("plant-1")?.health)
        assertEquals(1, database.plantDao().count())
        assertFalse(requireNotNull(database.dailyStatDao().getByDay(DAY_ONE)).gardenApplied)
    }

    @Test
    fun plantDeletionRemainsExplicit() = runTest {
        repository.savePlant(plant())
        repository.deletePlant("plant-1")

        assertNull(repository.getPlant("plant-1"))
        assertTrue(repository.observePlants().first().isEmpty())
    }

    @Test
    fun packSelectionOnlyAppliesToOneNewSeedAndNeverReplacesExistingPlants() = runTest {
        var selections = 0
        repository = GardenRepositoryImpl(database) { completedDay, previous ->
            assertEquals(DAY_ONE, completedDay.dayStartUtcMillis)
            assertEquals("fern", previous.single().species)
            selections++
            "flower"
        }
        repository.savePlant(plant())
        database.dailyStatDao().upsert(stat().copy(score = 100, usageIsBounded = true))
        assertTrue(repository.advanceCompletedDay(day(100)))
        assertFalse(repository.advanceCompletedDay(day(100)))
        assertEquals(1, selections)
        assertEquals("fern", repository.getPlant("plant-1")?.species)
        assertEquals(setOf("fern", "flower"), repository.observePlants().first().map { it.species }.toSet())
    }

    @Test
    fun focusUnlockCountUsesTerminalCompletionsBeforeTheExclusiveBoundary() = runTest {
        val completed = com.screengarden.app.data.local.entity.FocusSession(
            sessionId = "completed",
            dayStartUtcMillis = DAY_ONE,
            startedAtUtcMillis = DAY_ONE,
            endedAtUtcMillis = DAY_ONE + 1_000,
            plannedMinutes = 1,
            wasCompleted = true,
            status = "COMPLETED"
        )
        database.focusSessionDao().upsert(completed)
        database.focusSessionDao().upsert(completed.copy(sessionId = "later", endedAtUtcMillis = DAY_TWO))
        database.focusSessionDao().upsert(
            completed.copy(sessionId = "cancelled", status = "CANCELLED", wasCompleted = false)
        )
        database.focusSessionDao().upsert(
            completed.copy(sessionId = "running", status = "RUNNING", endedAtUtcMillis = null)
        )
        assertEquals(1, database.focusSessionDao().completedBefore(DAY_TWO))
        assertEquals(2, database.focusSessionDao().completedBefore(DAY_TWO + 1))
    }

    private fun stat(day: Long = DAY_ONE): DailyStat = DailyStat(dayStartUtcMillis = day, updatedAtUtcMillis = day)

    @Test
    fun snapshotsIncludeOnlyEnabledLimitOveragesForTheMatchingDay() = runTest {
        database.dailyStatDao().upsert(stat())
        database.appLimitDao().upsert(com.screengarden.app.data.local.entity.AppLimit("fake", 30, true, DAY_ONE))
        database.appLimitDao().upsert(com.screengarden.app.data.local.entity.AppLimit("paused", 1, false, DAY_ONE))
        database.appUsageDailyDao().upsert(
            com.screengarden.app.data.local.entity.AppUsageDaily(DAY_ONE, "fake", 90 * 60_000L, 1, DAY_ONE)
        )
        database.appUsageDailyDao().upsert(
            com.screengarden.app.data.local.entity.AppUsageDaily(DAY_TWO, "fake", 200 * 60_000L, 1, DAY_TWO)
        )
        assertEquals(
            listOf(60),
            repository.observeSnapshot(DAY_ONE, DAY_ONE).first().days.single().limitOveragesMinutes
        )
        database.appLimitDao().upsert(com.screengarden.app.data.local.entity.AppLimit("fake", 30, false, DAY_ONE))
        assertNull(repository.observeSnapshot(DAY_ONE, DAY_ONE).first().days.single().limitOveragesMinutes)
    }

    private fun plant(): Plant = Plant(id = "plant-1", species = "fern", plantedAtUtcMillis = DAY_ONE)

    private fun day(score: Int?): GardenDay = GardenDay(score = score, dayStartUtcMillis = DAY_ONE)

    private companion object {
        const val DAY_ONE = 1_700_000_000_000L
        const val DAY_TWO = 1_700_086_400_000L
    }
}
