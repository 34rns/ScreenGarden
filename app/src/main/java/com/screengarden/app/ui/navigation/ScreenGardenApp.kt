// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.screengarden.app.ui.screens.settings.SettingsScreen

/**
 * App shell: a [Scaffold] with the persistent bottom bar and the tab [ScreenGardenNavHost].
 *
 * [debugDestination] is null in every release build and in debug builds launched
 * without a debug extra, so the shipped navigation graph is exactly the four tabs.
 */
@Composable
fun ScreenGardenApp(
    modifier: Modifier = Modifier,
    debugDestination: DebugDestination? = null,
    gardenContent: @Composable (() -> Unit) -> Unit,
    focusContent: @Composable () -> Unit,
    limitsContent: @Composable (() -> Unit) -> Unit,
    habitsContent: @Composable (() -> Unit) -> Unit = { _ -> },
    insightsContent: @Composable (() -> Unit) -> Unit = { _ -> },
    historyContent: @Composable (() -> Unit) -> Unit = { _ -> },
    settingsContent: @Composable (() -> Unit, () -> Unit) -> Unit = { limits, habits ->
        SettingsScreen(limits, onMilestones = habits)
    }
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val currentDestination = debugDestination?.takeIf { it.route == route }?.selectedTab
        ?: ScreenGardenDestination.fromRoute(route)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            ScreenGardenBottomBar(
                currentDestination = currentDestination,
                onDestinationSelected = { destination ->
                    navController.navigate(destination.route) {
                        // Standard bottom-bar behaviour: one entry per tab, state kept
                        // per tab, and the back stack never grows by tapping tabs.
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        ScreenGardenNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            debugDestination = debugDestination,
            gardenContent = gardenContent,
            focusContent = focusContent,
            limitsContent = limitsContent,
            habitsContent = habitsContent,
            insightsContent = insightsContent,
            historyContent = historyContent,
            settingsContent = settingsContent
        )
    }
}
