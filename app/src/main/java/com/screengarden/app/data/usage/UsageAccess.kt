// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.usage

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * The one place that answers "may this app read screen time?" and "where does the
 * user fix that?".
 *
 * Usage access is an app-op, not a runtime permission: it cannot be requested with
 * a dialog, only granted by the user in system settings. Both the repository (to
 * report a state) and the future onboarding screen (P07, to offer a button) need
 * this, so it lives in one object instead of being duplicated (DECISION D47).
 */
object UsageAccess {

    /**
     * True when Usage access is granted.
     *
     * The app-op is the authoritative check. Some OEM builds report MODE_DEFAULT
     * even after the user granted access, so a non-empty stats query is accepted as
     * a second opinion rather than sending the user back to settings forever.
     *
     * The app must declare `android.permission.PACKAGE_USAGE_STATS` for this check
     * to be meaningful; the user still grants it manually (SECTION 9).
     */
    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(android.app.AppOpsManager::class.java) ?: return false
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        if (mode == android.app.AppOpsManager.MODE_ALLOWED) return true

        val usageStatsManager = context.getSystemService(android.app.usage.UsageStatsManager::class.java)
            ?: return false
        val now = System.currentTimeMillis()
        val recent = usageStatsManager.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            now - FALLBACK_WINDOW_MILLIS,
            now
        )
        return !recent.isNullOrEmpty()
    }

    /**
     * Opens the system screen where the user grants Usage access.
     *
     * The screen is a single device-wide list, so no package-specific data URI is
     * added: `Settings.ACTION_USAGE_ACCESS_SETTINGS` ignores it on most builds and
     * some throw on an unknown URI. Callers launching this from a non-Activity
     * context still need FLAG_ACTIVITY_NEW_TASK, which is why it is set here.
     */
    fun settingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Long enough that any use of the phone in the last hour shows up. */
    private const val FALLBACK_WINDOW_MILLIS: Long = 60L * 60L * 1000L
}
