// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.data.local.entity.DailyStat
import com.screengarden.app.domain.focus.FocusSessionMachine
import com.screengarden.app.domain.focus.FocusSessionState
import com.screengarden.app.domain.focus.FocusStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
class FocusSessionRepositoryImplTest {
    private lateinit var database: ScreenGardenDatabase
    private lateinit var repository: FocusSessionRepositoryImpl
    private val start = 1_799_424_000_000L
    private fun session(id: String = "focus") = FocusSessionState(id, LocalDay.startOfDay(start), start, 1)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ScreenGardenDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = FocusSessionRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun simultaneousStartsReturnTheSameRunningSession() = runTest {
        val sessions = listOf(async { repository.start(session("one")) }, async { repository.start(session("two")) })
            .awaitAll()
        assertEquals(sessions.first().id, sessions.last().id)
    }

    @Test
    fun completionCreditsOnceAndPreservesUsageAndGardenFields() = runTest {
        val day = LocalDay.startOfDay(start + 60_000)
        database.dailyStatDao().upsert(
            DailyStat(
                day,
                screenMinutes = 90,
                updatedAtUtcMillis = start,
                score = 80,
                gardenApplied = true
            )
        )
        repository.start(session())
        val complete = FocusSessionMachine.tick(session(), 60_000, start + 60_000)
        assertTrue(repository.checkpoint(complete))
        assertFalse(repository.checkpoint(complete))
        val total = requireNotNull(database.dailyStatDao().getByDay(day))
        assertEquals(1, total.focusMinutes)
        assertEquals(1, total.sessionsCompleted)
        assertEquals(90, total.screenMinutes)
        assertEquals(80, total.score)
        assertTrue(total.gardenApplied)
        assertNull(repository.activeSession())
    }

    @Test
    fun cancellationRejectsAnOverlappingTimerWriteAndEarnsNoCompletion() = runTest {
        repository.start(session())
        assertTrue(
            repository.checkpoint(
                FocusSessionMachine.end(
                    session(),
                    FocusStatus.CANCELLED,
                    10_000,
                    start + 10_000
                )
            )
        )
        assertFalse(repository.checkpoint(FocusSessionMachine.tick(session(), 60_000, start + 60_000)))
        assertEquals(FocusStatus.CANCELLED, repository.getSession("focus")?.status)
        assertNull(database.dailyStatDao().getByDay(LocalDay.startOfDay(start)))
    }

    @Test
    fun olderCheckpointsCannotMoveProgressBackward() = runTest {
        repository.start(session())
        assertTrue(repository.checkpoint(FocusSessionMachine.tick(session(), 20_000, start + 20_000)))
        assertFalse(repository.checkpoint(FocusSessionMachine.tick(session(), 10_000, start + 10_000)))
        assertEquals(20_000L, repository.getSession("focus")?.actualDurationMs)
    }

    @Test
    fun immediateGrowthAndHealingAreOnceOnlyWithTheTerminalWrite() = runTest {
        val garden = GardenRepositoryImpl(database)
        garden.savePlant(
            com.screengarden.app.domain.garden.Plant(
                "seed",
                "fern",
                44,
                10,
                com.screengarden.app.domain.garden.PlantCondition.WILTING,
                start
            )
        )
        repository.start(session())
        val complete = FocusSessionMachine.tick(session(), 60_000, start + 60_000)
        assertTrue(repository.checkpoint(complete))
        assertFalse(repository.checkpoint(complete))
        val grown = requireNotNull(garden.getPlant("seed"))
        assertEquals(46, grown.growthPoints)
        assertEquals(14, grown.health)
        assertEquals(start + 60_000, grown.lastFocusAtUtcMillis)
    }
}
