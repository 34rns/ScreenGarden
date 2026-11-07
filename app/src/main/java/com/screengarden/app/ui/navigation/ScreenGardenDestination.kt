// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.screengarden.app.R

/**
 * The four top-level destinations of the bottom navigation bar.
 *
 * Routes are plain strings on purpose: with a flat, argument-free tab set the
 * type-safe (serialization-based) navigation API adds a plugin and ceremony
 * without removing any real mistake.
 */
enum class ScreenGardenDestination(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    Garden(route = "garden", labelRes = R.string.tab_garden, icon = Icons.Filled.Home),
    Focus(route = "focus", labelRes = R.string.tab_focus, icon = Icons.Filled.PlayArrow),
    Insights(route = "insights", labelRes = R.string.tab_insights, icon = Icons.Filled.DateRange),
    Settings(route = "settings", labelRes = R.string.tab_settings, icon = Icons.Filled.Settings)
    ;

    companion object {
        /** Tab shown on cold start. */
        val START: ScreenGardenDestination = Garden

        /** Maps a nav back stack route back to a tab, defaulting to [START]. */
        fun fromRoute(route: String?): ScreenGardenDestination =
            if (route == "app_limits") Settings else entries.firstOrNull { it.route == route } ?: START
    }
}
