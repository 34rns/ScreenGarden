// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.usage

import android.content.Context
import android.content.Intent
import com.screengarden.app.domain.limits.LaunchableApp

/** Uses the existing MAIN/LAUNCHER queries declaration, never QUERY_ALL_PACKAGES. */
class LaunchableAppCatalog(context: Context) {
    private val manager = context.applicationContext.packageManager

    fun installedApps(): List<LaunchableApp> = manager.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
        0
    ).mapNotNull { resolved ->
        val info = resolved.activityInfo ?: return@mapNotNull null
        val label = try {
            info.applicationInfo.loadLabel(manager).toString().ifBlank { info.packageName }
        } catch (_: RuntimeException) {
            info.packageName
        }
        LaunchableApp(info.packageName, label)
    }.distinctBy { it.packageName }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
}
