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
        if (endMillis <= dayStartUtcMillis) {
            return DayUsageResult.Available(
                DayUsage(
                    dayStartUtcMillis,
                    0L,
                    0,
                    if (source.supportsUnlocks) UnlockCount.Supported(0) else UnlockCount.Unsupported,
                    emptyList()
                )
            )
        }
        val window = measureWindow(dayStartUtcMillis, endMillis, LocalDay.previousDayStart(dayStartUtcMillis, zoneId))
        return DayUsageResult.Available(
            DayUsage(
                dayStartUtcMillis,
                window.apps.sumOf { it.usageMillis },
                window.pickups,
                window.unlocks,
                window.apps,
                window.usageIsBounded
            )
        )
    }

    /** A focus window uses the same pairing rules; starting from the visible UI proves its initial screen state. */
    fun readWindow(startMillis: Long, endMillis: Long, screenOnAtStart: Boolean): UsageWindow? {
        val untilMillis = minOf(endMillis, nowMillis())
        if (!source.hasUsageAccess() || untilMillis <= startMillis) return null
        return measureWindow(startMillis, untilMillis, startMillis, screenOnAtStart)
    }

    private fun measureWindow(
        dayStartUtcMillis: Long,
        endMillis: Long,
        queryStart: Long,
        screenOnAtStart: Boolean = false
    ): UsageWindow {
        val launchablePackages = source.launchablePackages()
        val lastUsedByPackage = source.appTotals(dayStartUtcMillis, endMillis)
            .associate { it.packageName to it.lastTimeUsedMillis }

        val windows = LinkedHashMap<String, MutableWindow>()
        var openPackage: String? = null
        var openActivity: String? = null
        var openSinceMillis = dayStartUtcMillis
        var pickups = 0
        var unlocks = 0

        var interactive = true
        var hasScreenEvidence = screenOnAtStart
        // Daily reads replay yesterday; focus reads start at the user-initiated session.
        for (event in source.events(queryStart, endMillis).sortedBy { it.timestampMillis }) {
            val timestamp = event.timestampMillis
            if (timestamp < queryStart || timestamp >= endMillis) continue
            val inDay = timestamp >= dayStartUtcMillis

            when (event.type) {
                UsageEventType.SCREEN_INTERACTIVE -> {
                    hasScreenEvidence = true
                    if (inDay) pickups++
                    if (!interactive) openSinceMillis = timestamp
                    interactive = true
                }
                UsageEventType.SCREEN_NON_INTERACTIVE -> {
                    hasScreenEvidence = true
                    if (interactive) {
                        closeWindow(openPackage, openSinceMillis, timestamp, windows, dayStartUtcMillis)
                    }
                    interactive = false
                }
                UsageEventType.DEVICE_SHUTDOWN -> {
                    if (interactive) {
                        closeWindow(openPackage, openSinceMillis, timestamp, windows, dayStartUtcMillis)
                    }
                    openPackage = null
                    openActivity = null
                    interactive = false
                }
                UsageEventType.DEVICE_STARTUP -> {
                    // An unmatched interval's end before a restart is unknown.
                    openPackage = null
                    openActivity = null
                    interactive = false
                }
                UsageEventType.KEYGUARD_HIDDEN -> if (inDay) unlocks++
                UsageEventType.ACTIVITY_RESUMED -> {
                    val packageName = event.packageName ?: continue
                    if (packageName == openPackage) {
                        if (event.activityName != null) openActivity = event.activityName
                        continue
                    }
                    if (interactive) {
                        closeWindow(openPackage, openSinceMillis, timestamp, windows, dayStartUtcMillis)
                    }
                    openPackage = packageName
                    openActivity = event.activityName
                    openSinceMillis = timestamp
                    if (inDay) {
                        val window = windows.getOrPut(packageName) { MutableWindow() }
                        window.launchCount++
                        window.lastResumedMillis = timestamp
                    }
                }
                UsageEventType.ACTIVITY_PAUSED,
                UsageEventType.ACTIVITY_STOPPED -> {
                    if (openPackage != null &&
                        openPackage == event.packageName &&
                        (event.activityName == null || openActivity == null || event.activityName == openActivity)
                    ) {
                        if (interactive) {
                            closeWindow(openPackage, openSinceMillis, timestamp, windows, dayStartUtcMillis)
                        }
                        openPackage = null
                        openActivity = null
                    }
                }
            }
        }
        if (interactive) closeWindow(openPackage, openSinceMillis, endMillis, windows, dayStartUtcMillis)

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

        return UsageWindow(
            apps = apps,
            pickups = pickups,
            unlocks = if (source.supportsUnlocks) UnlockCount.Supported(unlocks) else UnlockCount.Unsupported,
            usageIsBounded = source.supportsScreenState && hasScreenEvidence
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

/** Measured transient interval; no invented day key and nothing is persisted by this read. */
data class UsageWindow(
    val apps: List<AppUsage>,
    val pickups: Int,
    val unlocks: UnlockCount,
    val usageIsBounded: Boolean
)
