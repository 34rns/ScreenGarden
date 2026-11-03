// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.debug

import android.content.Context
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.data.usage.AndroidUsageSource
import com.screengarden.app.ui.debug.UsageSelfCheckScreen
import com.screengarden.app.ui.navigation.DebugDestination

/**
 * The debug-build catalogue of diagnostic screens (P05b).
 *
 * This file exists only in `src/debug`, so the screen it references cannot reach a
 * release build. The release source set provides a stub with the same signature
 * that always returns null.
 *
 * Add a screen here rather than a second Activity: H5 keeps the app to one
 * Activity, and a debug destination keeps it that way (D49).
 */
object DebugCatalog {

    /** Route and intent-extra value for the usage self-check screen. */
    const val USAGE_SELF_CHECK: String = "usage_self_check"

    /** The requested debug destination, or null when [requested] names none. */
    fun destination(requested: String?, context: Context): DebugDestination? {
        if (requested != USAGE_SELF_CHECK) return null
        val appContext = context.applicationContext
        val container = (appContext as ScreenGardenApplication).container
        // Built once per launch so the screen reports the same source the app uses.
        val source = AndroidUsageSource(appContext)
        return DebugDestination(USAGE_SELF_CHECK) {
            UsageSelfCheckScreen(
                usageRepository = container.usageRepository,
                source = source
            )
        }
    }
}
