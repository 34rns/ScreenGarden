// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.onboarding

import com.screengarden.app.data.settings.SettingsRepository
import com.screengarden.app.domain.model.AccentColor
import com.screengarden.app.domain.model.OnboardingChoices
import com.screengarden.app.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The persistence contract of the first-run flow (P07).
 *
 * A plain JVM test over a fake repository: no DataStore, no Compose. What matters
 * here is the write order - onboarding is marked done last - because that rule is
 * what keeps a crash from locking the user into half-applied goals.
 */
class OnboardingCompletionTest {

    @Test
    fun completeStoresEveryGoalThenMarksOnboardingDone() = runTest {
        val settings = RecordingSettingsRepository()

        OnboardingCompletion(settings).complete(
            OnboardingChoices(
                dailyGoalMs = 90L * 60L * 1000L,
                pickupGoal = 25,
                bedtimeStartMinutes = 21 * 60,
                bedtimeEndMinutes = 6 * 60 + 30
            )
        )

        assertEquals(
            listOf("dailyGoalMs", "pickupGoal", "bedtimeStart", "bedtimeEnd", "onboardingDone"),
            settings.writes
        )
        assertEquals(90L * 60L * 1000L, settings.storedDailyGoalMs)
        assertEquals(25, settings.storedPickupGoal)
        assertEquals("21:00", settings.storedBedtimeStart)
        assertEquals("06:30", settings.storedBedtimeEnd)
        assertEquals(true, settings.storedOnboardingDone)
    }

    @Test
    fun theNotificationAnswerIsStoredAsGiven() = runTest {
        val settings = RecordingSettingsRepository()
        val completion = OnboardingCompletion(settings)

        completion.rememberNotifications(allowed = true)
        completion.rememberNotifications(allowed = false)

        assertEquals(
            listOf("notificationsOn", "notificationsOn"),
            settings.writes
        )
        assertEquals(false, settings.storedNotificationsOn)
    }

    /**
     * Records the order of writes and the last value of each key, which is the only
     * thing this class decides beyond passing values through.
     */
    private class RecordingSettingsRepository : SettingsRepository {

        val writes = mutableListOf<String>()
        var storedDailyGoalMs: Long? = null
        var storedPickupGoal: Int? = null
        var storedBedtimeStart: String? = null
        var storedBedtimeEnd: String? = null
        var storedNotificationsOn: Boolean? = null
        var storedOnboardingDone: Boolean? = null

        override val dailyGoalMs: Flow<Long> = flowOf(0L)
        override val pickupGoal: Flow<Int> = flowOf(0)
        override val bedtimeStart: Flow<String> = flowOf("00:00")
        override val bedtimeEnd: Flow<String> = flowOf("00:00")
        override val theme: Flow<ThemePreference> = flowOf(ThemePreference.SYSTEM)
        override val accentColor: Flow<AccentColor> = flowOf(AccentColor.CALM_GREEN)
        override val streakThreshold: Flow<Int> = flowOf(0)
        override val gentleMode: Flow<Boolean> = flowOf(true)
        override val notificationsOn: Flow<Boolean> = flowOf(false)
        override val onboardingDone: Flow<Boolean> = flowOf(false)
        override val keepScreenOn: Flow<Boolean> = flowOf(false)

        override suspend fun setKeepScreenOn(enabled: Boolean) = Unit

        override suspend fun setDailyGoalMs(goalMs: Long) {
            writes += "dailyGoalMs"
            storedDailyGoalMs = goalMs
        }

        override suspend fun setPickupGoal(goal: Int) {
            writes += "pickupGoal"
            storedPickupGoal = goal
        }

        override suspend fun setBedtimeStart(value: String) {
            writes += "bedtimeStart"
            storedBedtimeStart = value
        }

        override suspend fun setBedtimeEnd(value: String) {
            writes += "bedtimeEnd"
            storedBedtimeEnd = value
        }

        override suspend fun setTheme(preference: ThemePreference) = Unit

        override suspend fun setAccentColor(color: AccentColor) = Unit

        override suspend fun setStreakThreshold(threshold: Int) = Unit

        override suspend fun setGentleMode(enabled: Boolean) = Unit

        override suspend fun setNotificationsOn(enabled: Boolean) {
            writes += "notificationsOn"
            storedNotificationsOn = enabled
        }

        override suspend fun setOnboardingDone(done: Boolean) {
            writes += "onboardingDone"
            storedOnboardingDone = done
        }
    }
}
