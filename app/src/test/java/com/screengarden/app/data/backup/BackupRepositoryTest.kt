// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.data.local.entity.AppLimit
import com.screengarden.app.data.local.entity.AppUsageDaily
import com.screengarden.app.data.local.entity.Badge
import com.screengarden.app.data.local.entity.DailyStat
import com.screengarden.app.data.local.entity.FocusSession
import com.screengarden.app.data.local.entity.GardenHistory
import com.screengarden.app.data.local.entity.Plant
import com.screengarden.app.data.settings.DataStoreSettingsRepository
import com.screengarden.app.domain.backup.RestoreMode
import com.screengarden.app.domain.model.ThemePreference
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {
    private lateinit var database: ScreenGardenDatabase
    private lateinit var prefs: DataStore<Preferences>
    private lateinit var settings: DataStoreSettingsRepository
    private lateinit var scope: CoroutineScope
    private lateinit var repository: BackupRepository
    private val instant = Instant.parse("2026-11-01T12:00:00Z")
    private val day = LocalDay.startOfDay(instant.toEpochMilli())

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ScreenGardenDatabase::class.java).build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        prefs = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(context.cacheDir, "backup-${UUID.randomUUID()}.preferences_pb") }
        )
        settings = DataStoreSettingsRepository(prefs)
        repository = BackupRepository(database, prefs) { instant.toEpochMilli() + 120_000 }
    }

    @After
    fun cleanup() {
        database.close()
        scope.cancel()
    }

    private suspend fun seed(running: Boolean = false) {
        database.dailyStatDao().upsert(
            DailyStat(
                day, focusMinutes = if (running) 0 else 1, screenMinutes = 90,
                sessionsCompleted = if (running) 0 else 1, updatedAtUtcMillis = instant.toEpochMilli(), score = 84,
                usageIsBounded = true, gardenApplied = false, isCompassionDay = true
            )
        )
        database.plantDao().upsert(Plant("plant", "fern", 0, 60, instant.toEpochMilli()))
        database.appUsageDailyDao().upsert(AppUsageDaily(day, "fake.reader", 90 * 60_000L, 2, instant.toEpochMilli()))
        database.appLimitDao().upsert(AppLimit("fake.reader", 60, true, instant.toEpochMilli()))
        database.badgeDao().insertIfAbsent(Badge("first-bloom", instant.toEpochMilli()))
        database.gardenHistoryDao().insertIfAbsent(
            GardenHistory(
                day,
                instant.toEpochMilli(),
                84,
                "{\"version\":1,\"plants\":[]}"
            )
        )
        database.focusSessionDao().upsert(
            FocusSession(
                "session", day, instant.toEpochMilli(),
                endedAtUtcMillis = if (running) null else instant.toEpochMilli() + 60_000,
                plannedMinutes = 1, wasCompleted = !running, plantId = "plant", tag = "STUDY",
                status = if (running) "RUNNING" else "COMPLETED", actualDurationMs = 60_000,
                updatedAtUtcMillis = instant.toEpochMilli() + 60_000
            )
        )
        settings.setTheme(ThemePreference.DARK)
    }

    @Test
    fun replaceRestoresEveryUserTableAndSettingsWithoutRepeatingRewards() = runBlocking {
        seed()
        val bytes = repository.export()
        settings.setTheme(ThemePreference.LIGHT)
        database.plantDao().upsert(Plant("extra", "cactus", 0, 30, instant.toEpochMilli()))
        repository.restore(bytes, RestoreMode.REPLACE)
        val restored = repository.preview(repository.export())
        assertTrue(restored.counts.values.all { it == 1 })
        assertEquals(ThemePreference.DARK, settings.theme.first())
        assertFalse(database.dailyStatDao().getByDay(day)!!.gardenApplied)
        assertTrue(database.dailyStatDao().getByDay(day)!!.isCompassionDay)
        assertNull(database.plantDao().getById("extra"))
        assertNull(database.pendingRestoreDao().get())
    }

    @Test
    fun mergeKeepsExistingValuesAndSettingsAndIsIdempotent() = runBlocking {
        seed()
        val bytes = repository.export()
        database.plantDao().upsert(Plant("plant", "fern", 0, 99, instant.toEpochMilli()))
        settings.setTheme(ThemePreference.AMOLED)
        repository.restore(bytes, RestoreMode.MERGE)
        repository.restore(bytes, RestoreMode.MERGE)
        assertEquals(99, database.plantDao().getById("plant")!!.health)
        assertEquals(ThemePreference.AMOLED, settings.theme.first())
        assertEquals(1, database.dailyStatDao().getByDay(day)!!.sessionsCompleted)
        assertEquals(1, database.dailyStatDao().getByDay(day)!!.focusMinutes)
        assertTrue(repository.preview(repository.export()).counts.values.all { it == 1 })
    }

    @Test
    fun mergeDoesNotConsumeTodaysFutureGrowthButNeverReplaysOlderDays() = runBlocking {
        seed()
        val bytes = repository.export()
        database.dailyStatDao().deleteAll()
        repository.restore(bytes, RestoreMode.MERGE)
        assertFalse(database.dailyStatDao().getByDay(day)!!.gardenApplied)
        database.dailyStatDao().deleteAll()
        BackupRepository(database, prefs) { instant.plusSeconds(86_400).toEpochMilli() }
            .restore(bytes, RestoreMode.MERGE)
        assertTrue(database.dailyStatDao().getByDay(day)!!.gardenApplied)
    }

    @Test
    fun replacePreservesLegacySavedFocusTotalsAndPendingGrowthExactly() = runBlocking {
        seed()
        val original = database.dailyStatDao().getByDay(day)!!.copy(focusMinutes = 90, sessionsCompleted = 4)
        database.dailyStatDao().upsert(original)
        val bytes = repository.export()
        database.dailyStatDao().deleteAll()
        repository.restore(bytes, RestoreMode.REPLACE)
        assertEquals(original, database.dailyStatDao().getByDay(day))
    }

    @Test
    fun mergePreservesExistingLegacyTotalsAndDoesNotCreditConflictingDaysAgain() = runBlocking {
        seed()
        val bytes = repository.export()
        val original = database.dailyStatDao().getByDay(day)!!.copy(focusMinutes = 90, sessionsCompleted = 4)
        database.dailyStatDao().upsert(original)
        database.focusSessionDao().deleteAll()
        repository.restore(bytes, RestoreMode.MERGE)
        repository.restore(bytes, RestoreMode.MERGE)
        assertEquals(original, database.dailyStatDao().getByDay(day))
        assertNotNull(database.focusSessionDao().getById("session"))
    }

    @Test
    fun activeLocalSessionPreventsWritesAndImportedTimersAreStopped() = runBlocking {
        seed(running = true)
        val bytes = repository.export()
        var refused = false
        try {
            repository.restore(bytes, RestoreMode.REPLACE)
        } catch (_: IllegalStateException) {
            refused = true
        }
        assertTrue(refused)
        assertEquals("RUNNING", database.focusSessionDao().getById("session")!!.status)
        database.focusSessionDao().deleteAll()
        repository.restore(bytes, RestoreMode.REPLACE)
        assertNull(database.focusSessionDao().activeSession())
        assertEquals("CANCELLED", database.focusSessionDao().getById("session")!!.status)
        assertEquals(0, database.dailyStatDao().getByDay(day)!!.sessionsCompleted)
    }

    @Test
    fun damagedDocumentLeavesCurrentDataUntouched() = runBlocking {
        seed()
        var refused = false
        try {
            repository.restore("bad".toByteArray(), RestoreMode.REPLACE)
        } catch (
            _: IllegalArgumentException
        ) {
            refused = true
        }
        assertTrue(refused)
        assertEquals(60, database.plantDao().getById("plant")!!.health)
        assertEquals(ThemePreference.DARK, settings.theme.first())
    }

    @Test
    fun committedSettingsReceiptRecoversAfterRepositoryRecreation() = runBlocking {
        seed()
        val bytes = repository.export()
        settings.setTheme(ThemePreference.LIGHT)
        var fail = true
        val failing = object : DataStore<Preferences> by prefs {
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                if (fail) throw IOException("Fake disk failure")
                return prefs.updateData(transform)
            }
        }
        var refused = false
        try {
            BackupRepository(database, failing).restore(bytes, RestoreMode.REPLACE)
        } catch (
            _: IOException
        ) {
            refused = true
        }
        assertTrue(refused)
        assertNotNull(database.pendingRestoreDao().get())
        assertEquals(ThemePreference.LIGHT, settings.theme.first())
        fail = false
        BackupRepository(database, failing).recoverPendingSettings()
        assertEquals(ThemePreference.DARK, settings.theme.first())
        assertNull(database.pendingRestoreDao().get())
    }
}
