// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.screengarden.app.domain.garden.Hemisphere
import com.screengarden.app.domain.model.AccentColor
import com.screengarden.app.domain.model.OnboardingChoices
import com.screengarden.app.domain.model.ThemePreference
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the real Preferences DataStore against a temporary file, so the
 * serialization and the file format are covered instead of a fake map.
 *
 * The DataStore is configured exactly like the production delegate in
 * SettingsDataStore.kt, including the corruption handler, so a regression in that
 * configuration fails here first.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DataStoreSettingsRepositoryTest {
    @Test
    fun deskIsOptInAndClockChoiceSurvivesReopening() = runBlocking {
        val settings = open().repository
        assertFalse(settings.deskModeEnabled.first())
        assertTrue(settings.deskClock.first())
        settings.setDeskModeEnabled(true)
        settings.setDeskClock(false)
        closeAll()
        val reopened = open().repository
        assertTrue(reopened.deskModeEnabled.first())
        assertFalse(reopened.deskClock.first())
    }

    @Test
    fun curatedAccentsAndAtomicGoalsSurviveReopening() = runBlocking {
        val settings = open()
        val choices = OnboardingChoices(120 * 60_000L, 45, 21 * 60, 6 * 60)
        settings.repository.setGoals(choices)
        settings.repository.setTheme(ThemePreference.AMOLED)
        settings.repository.setAccentColor(AccentColor.HEATHER)
        closeAll()
        val reopened = open().repository
        assertEquals(choices.dailyGoalMs, reopened.dailyGoalMs.first())
        assertEquals(45, reopened.pickupGoal.first())
        assertEquals("21:00", reopened.bedtimeStart.first())
        assertEquals("06:00", reopened.bedtimeEnd.first())
        assertEquals(ThemePreference.AMOLED, reopened.theme.first())
        assertEquals(AccentColor.HEATHER, reopened.accentColor.first())
    }

    @Test
    fun invalidGoalBatchDoesNotPartiallyOverwriteSavedChoices() = runBlocking {
        val settings = open().repository
        val before = settings.dailyGoalMs.first()
        assertTrue(runCatching { settings.setGoals(OnboardingChoices(pickupGoal = -1)) }.isFailure)
        assertEquals(before, settings.dailyGoalMs.first())
        assertEquals(80, settings.pickupGoal.first())
    }

    @Test
    fun compassionAllowanceHasAStableDefaultAndValidatedUpdates() = runBlocking {
        val settings = open()
        assertEquals(3, settings.repository.compassionDaysPerMonth.first())
        settings.repository.setCompassionDaysPerMonth(0)
        assertEquals(0, settings.repository.compassionDaysPerMonth.first())
        settings.repository.setCompassionDaysPerMonth(10)
        assertEquals(10, settings.repository.compassionDaysPerMonth.first())
        assertTrue(runCatching { settings.repository.setCompassionDaysPerMonth(11) }.isFailure)
        assertTrue(runCatching { settings.repository.setCompassionDaysPerMonth(-1) }.isFailure)
        assertEquals(10, settings.repository.compassionDaysPerMonth.first())
    }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val openScopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        runBlocking { closeAll() }
    }

    @Test
    fun defaultsMatchTheDocumentedValues() {
        runBlocking {
            val settings = open()

            assertEquals(SettingsDefaults.DAILY_GOAL_MS, settings.repository.dailyGoalMs.first())
            assertEquals(3L * 60L * 60L * 1000L, settings.repository.dailyGoalMs.first())
            assertEquals(80, settings.repository.pickupGoal.first())
            assertEquals("22:30", settings.repository.bedtimeStart.first())
            assertEquals("07:00", settings.repository.bedtimeEnd.first())
            assertEquals(ThemePreference.SYSTEM, settings.repository.theme.first())
            assertEquals(AccentColor.CALM_GREEN, settings.repository.accentColor.first())
            assertEquals(60, settings.repository.streakThreshold.first())
            assertTrue(settings.repository.gentleMode.first())
            assertFalse(settings.repository.notificationsOn.first())
            assertFalse(settings.repository.onboardingDone.first())
            assertFalse(settings.repository.keepScreenOn.first())
            assertEquals(Hemisphere.NORTHERN, settings.repository.hemisphere.first())
            assertFalse(settings.repository.lowPowerVisuals.first())
        }
    }

    @Test
    fun updatesAreEmittedBackByTheSameFlows() {
        runBlocking {
            val settings = open()

            settings.repository.setDailyGoalMs(90L * 60L * 1000L)
            settings.repository.setPickupGoal(40)
            settings.repository.setBedtimeStart("21:15")
            settings.repository.setBedtimeEnd("06:45")
            settings.repository.setTheme(ThemePreference.DARK)
            settings.repository.setStreakThreshold(75)
            settings.repository.setGentleMode(false)
            settings.repository.setNotificationsOn(true)
            settings.repository.setOnboardingDone(true)
            settings.repository.setKeepScreenOn(true)

            assertEquals(90L * 60L * 1000L, settings.repository.dailyGoalMs.first())
            assertEquals(40, settings.repository.pickupGoal.first())
            assertEquals("21:15", settings.repository.bedtimeStart.first())
            assertEquals("06:45", settings.repository.bedtimeEnd.first())
            assertEquals(ThemePreference.DARK, settings.repository.theme.first())
            assertEquals(75, settings.repository.streakThreshold.first())
            assertFalse(settings.repository.gentleMode.first())
            assertTrue(settings.repository.notificationsOn.first())
            assertTrue(settings.repository.onboardingDone.first())
            assertTrue(settings.repository.keepScreenOn.first())
        }
    }

    @Test
    fun amoledAndAccentChoicesSurviveAWrite() {
        runBlocking {
            val settings = open()

            settings.repository.setTheme(ThemePreference.AMOLED)
            settings.repository.setAccentColor(AccentColor.CALM_GREEN)

            assertEquals(ThemePreference.AMOLED, settings.repository.theme.first())
            assertEquals(AccentColor.CALM_GREEN, settings.repository.accentColor.first())
        }
    }

    @Test
    fun valuesSurviveReopeningTheSameFile() {
        runBlocking {
            val file = File(temporaryFolder.root, "persisted.preferences_pb")
            val first = open(file)
            first.repository.setPickupGoal(42)
            first.repository.setTheme(ThemePreference.LIGHT)
            first.repository.setHemisphere(Hemisphere.SOUTHERN)
            first.repository.setLowPowerVisuals(true)
            closeAll()

            val second = open(file)

            assertEquals(42, second.repository.pickupGoal.first())
            assertEquals(ThemePreference.LIGHT, second.repository.theme.first())
            assertEquals(Hemisphere.SOUTHERN, second.repository.hemisphere.first())
            assertTrue(second.repository.lowPowerVisuals.first())
        }
    }

    @Test
    fun unknownStoredValuesFallBackToDefaults() {
        runBlocking {
            val settings = open()
            settings.dataStore.edit {
                it[SettingsKeys.THEME] = "sepia"
                it[SettingsKeys.ACCENT_COLOR] = "neon_pink"
                it[SettingsKeys.HEMISPHERE] = "unknown"
            }

            assertEquals(ThemePreference.SYSTEM, settings.repository.theme.first())
            assertEquals(AccentColor.CALM_GREEN, settings.repository.accentColor.first())
            assertEquals(Hemisphere.NORTHERN, settings.repository.hemisphere.first())
        }
    }

    @Test
    fun aCorruptFileIsReplacedByDefaultsInsteadOfThrowing() {
        runBlocking {
            val file = File(temporaryFolder.root, "corrupt.preferences_pb")
            // Truncated protobuf: a declared length of 127 bytes with three left.
            file.writeBytes(byteArrayOf(0x0A, 0x7F, 0x7F, 0x7F))

            val settings = open(file)

            assertEquals(SettingsDefaults.DAILY_GOAL_MS, settings.repository.dailyGoalMs.first())
            assertEquals(ThemePreference.SYSTEM, settings.repository.theme.first())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun freeTextBedtimeIsRejected() {
        runBlocking { open().repository.setBedtimeStart("late") }
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroDailyGoalIsRejected() {
        runBlocking { open().repository.setDailyGoalMs(0L) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun streakThresholdOutsideTheScoreRangeIsRejected() {
        runBlocking { open().repository.setStreakThreshold(101) }
    }

    private fun open(file: File = File(temporaryFolder.root, "settings.preferences_pb")): UnderTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        openScopes += scope
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = scope,
            produceFile = { file }
        )
        return UnderTest(DataStoreSettingsRepository(dataStore), dataStore)
    }

    /** DataStore allows one instance per file, so every instance keeps its scope. */
    private suspend fun closeAll() {
        openScopes.forEach { scope -> scope.coroutineContext[Job]?.cancelAndJoin() }
        openScopes.clear()
    }

    private class UnderTest(val repository: DataStoreSettingsRepository, val dataStore: DataStore<Preferences>)
}
