// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.settings

import com.screengarden.app.domain.model.AccentColor
import com.screengarden.app.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

/**
 * User settings, persisted on device (AGENT_MEMORY SECTION 16.7).
 *
 * Every key is exposed as a [Flow] rather than a one-shot read so a screen that
 * is already visible updates the moment a value changes. Each flow always emits
 * a value: an absent key reports its [SettingsDefaults] entry, never `null`, so
 * callers never have to branch on "not set yet".
 *
 * Nothing here invents a key. A new setting must be added to SECTION 16.7 first
 * (that section is the contract for the stored shape).
 */
interface SettingsRepository {

    /** Screen-time goal for a whole local day, in milliseconds. */
    val dailyGoalMs: Flow<Long>

    /** Target number of phone pickups per local day. */
    val pickupGoal: Flow<Int>

    /** Bedtime window start, local time formatted HH:mm. */
    val bedtimeStart: Flow<String>

    /** Bedtime window end, local time formatted HH:mm. */
    val bedtimeEnd: Flow<String>

    /** Colour scheme choice. */
    val theme: Flow<ThemePreference>

    /** Calm palette variant. */
    val accentColor: Flow<AccentColor>

    /** Minimum daily health score that keeps a streak alive. */
    val streakThreshold: Flow<Int>

    /** Gentle mode: soft failures and no blocking. */
    val gentleMode: Flow<Boolean>

    /** Whether the user agreed to gentle reminders. */
    val notificationsOn: Flow<Boolean>

    /** Whether the first-run flow has been completed. */
    val onboardingDone: Flow<Boolean>

    val keepScreenOn: Flow<Boolean>

    /** @throws IllegalArgumentException when [goalMs] is not positive. */
    suspend fun setDailyGoalMs(goalMs: Long)

    /** @throws IllegalArgumentException when [goal] is negative. */
    suspend fun setPickupGoal(goal: Int)

    /** @throws IllegalArgumentException when [value] is not a HH:mm local time. */
    suspend fun setBedtimeStart(value: String)

    /** @throws IllegalArgumentException when [value] is not a HH:mm local time. */
    suspend fun setBedtimeEnd(value: String)

    suspend fun setTheme(preference: ThemePreference)

    suspend fun setAccentColor(color: AccentColor)

    /** @throws IllegalArgumentException when [threshold] is outside 0..100. */
    suspend fun setStreakThreshold(threshold: Int)

    suspend fun setGentleMode(enabled: Boolean)

    suspend fun setNotificationsOn(enabled: Boolean)

    suspend fun setOnboardingDone(done: Boolean)

    suspend fun setKeepScreenOn(enabled: Boolean)
}
