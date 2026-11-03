// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.settings

import com.screengarden.app.domain.model.AccentColor
import com.screengarden.app.domain.model.ThemePreference

/**
 * Defaults for every settings key in AGENT_MEMORY SECTION 16.7.
 *
 * They live in one place so the repository, the (future) settings UI and the
 * tests cannot drift: a mismatch here would show a value to the user that the
 * app does not actually use.
 *
 * The numbers are deliberately forgiving rather than aspirational - this app is
 * meant to feel calm, so the defaults assume the user is not failing anything.
 */
object SettingsDefaults {

    /** Screen-time goal per day: three hours. */
    const val DAILY_GOAL_MS: Long = 3 * 60 * 60 * 1000L

    /** Target pickups per day. */
    const val PICKUP_GOAL: Int = 80

    /** Bedtime window start, local time in HH:mm. */
    const val BEDTIME_START: String = "22:30"

    /** Bedtime window end, local time in HH:mm. */
    const val BEDTIME_END: String = "07:00"

    /** Follow the device dark setting until the user chooses otherwise. */
    val THEME: ThemePreference = ThemePreference.SYSTEM

    /** The only palette today. */
    val ACCENT_COLOR: AccentColor = AccentColor.CALM_GREEN

    /** Minimum daily health score that keeps a streak alive (16.4). */
    const val STREAK_THRESHOLD: Int = 60

    /** No blocking, no shaming: the product default (16.3). */
    const val GENTLE_MODE: Boolean = true

    /** Reminders stay off until the user asks for them (H4, H9). */
    const val NOTIFICATIONS_ON: Boolean = false

    /** Onboarding has not run yet; P07 flips this. */
    const val ONBOARDING_DONE: Boolean = false
}
