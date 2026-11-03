// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.usage

import com.screengarden.app.core.time.LocalDay
import java.time.ZoneId

/**
 * Turns raw OS events into one day of screen time.
 *
 * All the arithmetic lives here, behind [UsageSource], so it can be unit tested
 * with a fake on the JVM: day boundaries, resume/pause pairing, clipping, and the
 * launcher filter. The Android system service itself is only touched by
 * [AndroidUsageSource].
 *
 * Rules that matter:
 * - The window is `[start, nextDayStart)`; an event outside it is ignored even if a
 *   source returns more than asked (defensive, because bucket-based sources do).
 * - An interval still open at the end of the day is closed at the day boundary, so
 *   "used the phone at 23:58" counts for that day instead of vanishing. A day that
 *   has not finished yet is closed at "now" instead: otherwise an app left in the
 *   foreground counts every remaining hour of the day it has not used (K38).
 * - A resume for an app that is already open is ignored, and a pause without a
 *   matching resume is ignored: both happen in real event streams.
 * - Only launcher apps are reported, and the total is their sum (D44).
 * - Unlocks are reported as [UnlockCount.Unsupported] on platforms that have no
 *   keyguard event, never as a misleading 0 (D48).
 */
class DefaultUsageRepository(
    private val source: UsageSource,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : UsageRepository {

    override suspend fun isUsageAccessGranted(): Boolean = source.hasUsageAccess()

    override suspend fun readDay(dayStartUtcMillis: Long, zoneId: ZoneId): DayUsageResult {
        // Checked before any query: without access the system returns empty data,
        // which would look exactly like "you did not use your phone today".
        if (!source.hasUsageAccess()) return DayUsageResult.MissingPermission

        val dayEndMillis = LocalDay.nextDayStart(dayStartUtcMillis, zoneId)
        // Reading today (or a future day) must never invent time that has not
        // happened yet: the window stops at the earlier of the day boundary and now.
        val endMillis = minOf(dayEndMillis, nowMillis())
        val launchablePackages = source.launchablePackages()
        val lastUsedByPackage = source.appTotals(dayStartUtcMillis, endMillis)
            .associate { it.packageName to it.lastTimeUsedMillis }

        val windows = LinkedHashMap<String, MutableWindow>()
        var openPackage: String? = null
        var openSinceMillis = dayStartUtcMillis
        var pickups = 0
        var unlocks = 0

        for (event in source.events(dayStartUtcMillis, endMillis).sortedBy { it.timestampMillis }) {
            val timestamp = event.timestampMillis
            if (timestamp < dayStartUtcMillis || timestamp >= endMillis) continue

            when (event.type) {
                UsageEventType.SCREEN_INTERACTIVE -> pickups++
                UsageEventType.KEYGUARD_HIDDEN -> unlocks++
                UsageEventType.ACTIVITY_RESUMED -> {
                    val packageName = event.packageName ?: continue
                    if (packageName == openPackage) continue
                    closeWindow(openPackage, openSinceMillis, timestamp, windows, dayStartUtcMillis)
                    openPackage = packageName
                    openSinceMillis = timestamp
                    val window = windows.getOrPut(packageName) { MutableWindow() }
                    window.launchCount++
                    window.lastResumedMillis = timestamp
                }
                UsageEventType.ACTIVITY_PAUSED -> {
                    if (openPackage != null && openPackage == event.packageName) {
                        closeWindow(openPackage, openSinceMillis, timestamp, windows, dayStartUtcMillis)
                        openPackage = null
                    }
                }
            }
        }
        closeWindow(openPackage, openSinceMillis, endMillis, windows, dayStartUtcMillis)

        val apps = windows
            .filterKeys { it in launchablePackages }
            .map { (packageName, window) ->
                AppUsage(
                    packageName = packageName,
                    usageMillis = window.foregroundMillis,
                    launchCount = window.launchCount,
                    // The system's own "last used" wins; the last resume we saw is the
                    // fallback for sources that do not report totals.
                    lastUsedUtcMillis = lastUsedByPackage[packageName] ?: window.lastResumedMillis
                )
            }
            .sortedWith(compareByDescending<AppUsage> { it.usageMillis }.thenBy { it.packageName })

        return DayUsageResult.Available(
            DayUsage(
                dayStartUtcMillis = dayStartUtcMillis,
                totalScreenTimeMillis = apps.sumOf { it.usageMillis },
                pickups = pickups,
                // A platform without KEYGUARD_HIDDEN cannot count unlocks, so it says
                // so instead of reporting zero (D48).
                unlocks = if (source.supportsUnlocks) {
                    UnlockCount.Supported(unlocks)
                } else {
                    UnlockCount.Unsupported
                },
                apps = apps
            )
        )
    }

    /** Adds the slice of the open interval that falls inside the requested day. */
    private fun closeWindow(
        packageName: String?,
        sinceMillis: Long,
        untilMillis: Long,
        windows: MutableMap<String, MutableWindow>,
        windowStartMillis: Long
    ) {
        if (packageName == null) return
        val from = maxOf(sinceMillis, windowStartMillis)
        if (untilMillis <= from) return
        windows.getOrPut(packageName) { MutableWindow() }.foregroundMillis += untilMillis - from
    }

    private class MutableWindow {
        var foregroundMillis: Long = 0L
        var launchCount: Int = 0
        var lastResumedMillis: Long = 0L
    }
}
