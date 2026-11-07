// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.screengarden.app.ui.screens.insights.InsightsScreen
import com.screengarden.app.ui.screens.settings.SettingsScreen

/**
 * Hosts the four top-level screens, plus at most one debug-only destination when a
 * debug build was launched with the matching extra (P05b).
 */
@Composable
fun ScreenGardenNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    debugDestination: DebugDestination? = null,
    gardenContent: @Composable (() -> Unit) -> Unit,
    focusContent: @Composable () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = debugDestination?.route ?: ScreenGardenDestination.START.route,
        modifier = modifier
    ) {
        composable(ScreenGardenDestination.Garden.route) {
            gardenContent { navController.navigate(ScreenGardenDestination.Focus.route) { launchSingleTop = true } }
        }
        composable(ScreenGardenDestination.Focus.route) {
            focusContent()
        }
        composable(ScreenGardenDestination.Insights.route) {
            InsightsScreen()
        }
        composable(ScreenGardenDestination.Settings.route) {
            SettingsScreen()
        }
        if (debugDestination != null) {
            composable(debugDestination.route) {
                debugDestination.content()
            }
        }
    }
}
