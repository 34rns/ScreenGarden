// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.usage

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Which mechanism produced a [UsageAccessCheck].
 *
 * The app-op is authoritative; the stats query is only a second opinion for OEM
 * builds that leave the op at MODE_DEFAULT after the user granted access (D47).
 * Naming the method keeps that fallback honest and lets the debug self-check show
 * which one fired instead of hiding it.
 */
enum class PermissionCheckMethod {

    /** `AppOpsManager.unsafeCheckOpNoThrow` (API 29+). */
    APP_OPS_UNSAFE,

    /** `AppOpsManager.checkOpNoThrow` (API 26-28). */
    APP_OPS_LEGACY,

    /** The app-op did not say allowed, but a stats query returned data. */
    STATS_FALLBACK,

    /** No AppOpsManager was available, or it reported denied and no data came back. */
    NONE
}

/**
 * The result of the usage-access check, with the evidence that produced it.
 *
 * [appOpMode] is the raw [AppOpsManager] mode (-1 when unavailable), which is the
 * difference between "the user denied it" and "this build reports MODE_DEFAULT".
 */
data class UsageAccessCheck(val granted: Boolean, val appOpMode: Int, val method: PermissionCheckMethod)

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

    /** True when Usage access is granted, by either mechanism. */
    fun isGranted(context: Context): Boolean = check(context).granted

    /**
     * The full check, including how it was answered.
     *
     * Declaring `android.permission.PACKAGE_USAGE_STATS` is what makes the app-op
     * meaningful; the user still grants it manually (SECTION 9).
     */
    fun check(context: Context): UsageAccessCheck {
        val appOps = context.getSystemService(AppOpsManager::class.java)
            ?: return UsageAccessCheck(
                granted = false,
                appOpMode = MODE_UNAVAILABLE,
                method = PermissionCheckMethod.NONE
            )

        val unsafe = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val mode = if (unsafe) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        val appOpMethod = if (unsafe) PermissionCheckMethod.APP_OPS_UNSAFE else PermissionCheckMethod.APP_OPS_LEGACY
        if (mode == AppOpsManager.MODE_ALLOWED) {
            return UsageAccessCheck(granted = true, appOpMode = mode, method = appOpMethod)
        }

        // Some OEM builds keep reporting MODE_DEFAULT after the user granted access.
        // A non-empty stats query is accepted as a second opinion rather than sending
        // the user back to a settings screen that already shows "allowed".
        val usageStatsManager = context.getSystemService(android.app.usage.UsageStatsManager::class.java)
            ?: return UsageAccessCheck(granted = false, appOpMode = mode, method = PermissionCheckMethod.NONE)
        val now = System.currentTimeMillis()
        val recent = usageStatsManager.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            now - FALLBACK_WINDOW_MILLIS,
            now
        )
        return if (!recent.isNullOrEmpty()) {
            UsageAccessCheck(granted = true, appOpMode = mode, method = PermissionCheckMethod.STATS_FALLBACK)
        } else {
            UsageAccessCheck(granted = false, appOpMode = mode, method = PermissionCheckMethod.NONE)
        }
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

    /** Sentinel for [UsageAccessCheck.appOpMode] when AppOpsManager is missing. */
    private const val MODE_UNAVAILABLE: Int = -1

    /** Long enough that any use of the phone in the last hour shows up. */
    private const val FALLBACK_WINDOW_MILLIS: Long = 60L * 60L * 1000L
}
