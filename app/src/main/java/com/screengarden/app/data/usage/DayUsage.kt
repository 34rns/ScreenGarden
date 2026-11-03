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
 * One local day of screen time, the unit Insights and the daily sync consume.
 *
 * [totalScreenTimeMillis] is the sum of [apps], i.e. launcher apps only, so the
 * total always equals what a user can add up from the list (D44).
 */
data class DayUsage(
    val dayStartUtcMillis: Long,
    val totalScreenTimeMillis: Long,
    val pickups: Int,
    val unlocks: Int,
    val apps: List<AppUsage>
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
