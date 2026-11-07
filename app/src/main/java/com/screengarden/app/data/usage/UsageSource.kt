// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.usage

/**
 * The raw facts the operating system is willing to tell us about app usage.
 *
 * Everything below this interface is pure Kotlin and unit-testable; everything
 * above it is [AndroidUsageSource], the only class that touches UsageStatsManager,
 * PackageManager or AppOpsManager (DECISION D43).
 *
 * The methods are synchronous on purpose: UsageStatsManager's own queries are
 * synchronous, and a fake has nothing to suspend on. [UsageRepository] is the
 * suspending layer, so callers still never block the main thread by accident.
 */
interface UsageSource {

    /** Screen interactive/non-interactive events exist from Android API 28. */
    val supportsScreenState: Boolean

    /**
     * False when this platform cannot report unlocks at all.
     *
     * `UsageEvents.Event.KEYGUARD_HIDDEN` arrived in API 28, so on API 26-27 the
     * event simply never occurs. The source states that as a capability rather
     * than the repository guessing from a zero count (D48).
     */
    val supportsUnlocks: Boolean

    /** True when the user has granted Usage access to this app. */
    fun hasUsageAccess(): Boolean

    /**
     * Packages that appear in the launcher.
     *
     * Apps without a launcher icon are not shown to the user anywhere, so they are
     * excluded from screen time: system UI, the launcher itself and services would
     * otherwise dominate the numbers (DECISION D44).
     */
    fun launchablePackages(): Set<String>

    /** Per-app totals the system already keeps for the window (see [AppTotal]). */
    fun appTotals(startMillis: Long, endMillis: Long): List<AppTotal>

    /** Events in the window, ascending by timestamp, already mapped to [UsageEventType]. */
    fun events(startMillis: Long, endMillis: Long): List<UsageEvent>
}

/**
 * One app's entry in the system's usage stats.
 *
 * [lastTimeUsedMillis] is a *timestamp*, not a duration. It is exposed separately
 * so the difference stays visible: subtracting two of them is not screen time.
 * [totalTimeInForegroundMillis] is the system's own bucket total; the repository
 * does not trust it for a specific day because daily buckets are aligned to the
 * device's calendar day and can cover more than the requested window.
 */
data class AppTotal(val packageName: String, val lastTimeUsedMillis: Long, val totalTimeInForegroundMillis: Long)

/** A single foreground-related event, with the platform's int constants hidden. */
data class UsageEvent(
    val type: UsageEventType,
    val timestampMillis: Long,
    val packageName: String?,
    val activityName: String? = null
)

/**
 * The event kinds this app cares about.
 *
 * Deliberately small: every extra kind is another thing a fake has to fake, and
 * nothing in the product reads the rest.
 */
enum class UsageEventType {

    /** An app's activity came to the foreground (the start of a usage interval). */
    ACTIVITY_RESUMED,

    /** The foreground activity left again (the end of a usage interval). */
    ACTIVITY_PAUSED,

    /** Activity became invisible (API 29+). */
    ACTIVITY_STOPPED,

    /** The screen became interactive - one "pickup". */
    SCREEN_INTERACTIVE,

    /** Screen turned off or entered ambient display (API 28+). */
    SCREEN_NON_INTERACTIVE,

    /** Runtime shutdown closes all activity intervals (API 29+). */
    DEVICE_SHUTDOWN,

    /** Runtime startup invalidates any unfinished interval (API 29+). */
    DEVICE_STARTUP,

    /** The keyguard was dismissed - one "unlock" (API 28+ only, see D48). */
    KEYGUARD_HIDDEN
}
