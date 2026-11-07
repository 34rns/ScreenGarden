// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.usage

/**
 * What one app did during one local day.
 *
 * [usageMillis] and [lastUsedUtcMillis] answer different questions on purpose:
 * the first is time spent in the foreground, measured from resume/pause pairs;
 * the second is the instant the app was last in the foreground. A "last used"
 * timestamp is not a duration, and subtracting two of them is the classic way to
 * inflate screen time (DECISION D45).
 */
data class AppUsage(val packageName: String, val usageMillis: Long, val launchCount: Int, val lastUsedUtcMillis: Long)

/**
 * How many times the keyguard was dismissed during the day.
 *
 * Unlocking is only observable from API 28: `UsageEvents.Event.KEYGUARD_HIDDEN`
 * did not exist before that, and no earlier event reliably means "the user
 * unlocked the phone" (SCREEN_INTERACTIVE also fires for a plain wake). Reporting
 * a fabricated 0 on API 26-27 would be indistinguishable from "never unlocked",
 * so the absence is modelled as a state instead of a number (DECISION D48).
 */
sealed interface UnlockCount {

    /** The platform reports unlocks and this is the count for the day. */
    data class Supported(val count: Int) : UnlockCount

    /** The platform has no keyguard event, so unlocks cannot be counted. */
    data object Unsupported : UnlockCount
}

/**
 * One local day of screen time, the unit Insights and the daily sync consume.
 *
 * [totalScreenTimeMillis] is the sum of [apps], i.e. launcher apps only, so the
 * total always equals what a user can add up from the list (D44).
 */
data class DayUsage(
    val dayStartUtcMillis: Long,
    val totalScreenTimeMillis: Long,
    val pickups: Int,
    val unlocks: UnlockCount,
    val apps: List<AppUsage>,
    /** False when the event stream cannot bound activity time with screen state. */
    val usageIsBounded: Boolean = false
) {

    /** The entry for one package, or null when it was not used that day. */
    fun app(packageName: String): AppUsage? = apps.firstOrNull { it.packageName == packageName }

    /** The [limit] heaviest apps, already sorted by usage descending. */
    fun topApps(limit: Int): List<AppUsage> = apps.take(limit.coerceAtLeast(0))
}

/**
 * The answer to "what happened on this day".
 *
 * Permission is a state, not an exception: the UI has to render a "grant access"
 * card, so [MissingPermission] is a normal result rather than a crash (DECISION
 * D46).
 */
sealed interface DayUsageResult {

    /** Usage access is granted and the window was read. */
    data class Available(val usage: DayUsage) : DayUsageResult

    /** Usage access is not granted, so no numbers exist. Never an error. */
    data object MissingPermission : DayUsageResult
}
