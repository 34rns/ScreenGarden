// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.screengarden.app.R
import com.screengarden.app.ui.components.PlaceholderScreen
import com.screengarden.app.ui.navigation.ScreenGardenDestination

/** Settings tab placeholder. Real content arrives with the settings feature. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.tab_settings),
        description = stringResource(R.string.settings_placeholder),
        icon = ScreenGardenDestination.Settings.icon,
        modifier = modifier
    )
}
