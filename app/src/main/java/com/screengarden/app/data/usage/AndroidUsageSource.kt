// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent

/**
 * The only class in the app that talks to UsageStatsManager (DECISION D43).
 *
 * Everything it returns is plain data ([UsageSource]), so the aggregation rules
 * are testable on the JVM and this class stays a thin, boring adapter.
 *
 * Platform notes, including the parts that are not fully certain:
 * - `ACTIVITY_RESUMED` / `ACTIVITY_PAUSED` were added in API 29. They are the
 *   renamed forms of the deprecated `MOVE_TO_FOREGROUND` / `MOVE_TO_BACKGROUND`,
 *   which the platform still reports on older releases; both names carry the same
 *   constant values, so matching the new names also matches pre-29 events. This is
 *   documented behaviour, but it was only exercised on API 35 here (K32).
 * - `KEYGUARD_HIDDEN` exists from API 28. On API 26-27 the framework has no
 *   keyguard event in UsageEvents, so [UsageEventType.KEYGUARD_HIDDEN] simply never
 *   arrives there and unlocks stay 0 instead of being guessed from clock changes.
 * - `queryUsageStats` returns *buckets*, not an exact window: a daily bucket is
 *   aligned to the device's local day, so its totals can extend past the requested
 *   window. That is why this class only exposes the system's `lastTimeUsed` and the
 *   repository measures durations from events instead (D45).
 */
class AndroidUsageSource(context: Context) : UsageSource {

    private val appContext: Context = context.applicationContext

    private val usageStatsManager: UsageStatsManager? =
        appContext.getSystemService(UsageStatsManager::class.java)

    private val packageManager = appContext.packageManager

    override fun hasUsageAccess(): Boolean = UsageAccess.isGranted(appContext)

    override fun launchablePackages(): Set<String> {
        // Requires the <queries> block in the manifest on API 30+, otherwise this
        // returns only this app. See AndroidManifest.xml.
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }

    override fun appTotals(startMillis: Long, endMillis: Long): List<AppTotal> {
        val manager = usageStatsManager ?: return emptyList()
        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startMillis, endMillis)
            ?: return emptyList()
        return stats.mapNotNull { stat ->
            val packageName = stat.packageName ?: return@mapNotNull null
            AppTotal(
                packageName = packageName,
                lastTimeUsedMillis = stat.lastTimeUsed,
                totalTimeInForegroundMillis = stat.totalTimeInForeground
            )
        }
    }

    override fun events(startMillis: Long, endMillis: Long): List<UsageEvent> {
        val manager = usageStatsManager ?: return emptyList()
        val usageEvents = manager.queryEvents(startMillis, endMillis) ?: return emptyList()
        val event = UsageEvents.Event()
        val mapped = ArrayList<UsageEvent>()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val type = mapEventType(event.eventType) ?: continue
            mapped += UsageEvent(
                type = type,
                timestampMillis = event.timeStamp,
                packageName = event.packageName
            )
        }
        return mapped
    }

    private fun mapEventType(rawType: Int): UsageEventType? = when (rawType) {
        UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.ACTIVITY_RESUMED
        UsageEvents.Event.ACTIVITY_PAUSED -> UsageEventType.ACTIVITY_PAUSED
        UsageEvents.Event.SCREEN_INTERACTIVE -> UsageEventType.SCREEN_INTERACTIVE
        UsageEvents.Event.KEYGUARD_HIDDEN -> UsageEventType.KEYGUARD_HIDDEN
        else -> null
    }
}
