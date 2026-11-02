// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/** Bottom navigation bar listing every [ScreenGardenDestination] in order. */
@Composable
fun ScreenGardenBottomBar(
    currentDestination: ScreenGardenDestination,
    onDestinationSelected: (ScreenGardenDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        ScreenGardenDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == currentDestination,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        // The label below already announces the tab.
                        contentDescription = null
                    )
                },
                label = { Text(text = stringResource(destination.labelRes)) }
            )
        }
    }
}
