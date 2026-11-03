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

/**
 * App shell: a [Scaffold] with the persistent bottom bar and the tab [ScreenGardenNavHost].
 *
 * [debugDestination] is null in every release build and in debug builds launched
 * without a debug extra, so the shipped navigation graph is exactly the four tabs.
 */
@Composable
fun ScreenGardenApp(modifier: Modifier = Modifier, debugDestination: DebugDestination? = null) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = ScreenGardenDestination.fromRoute(backStackEntry?.destination?.route)

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
            debugDestination = debugDestination
        )
    }
}
