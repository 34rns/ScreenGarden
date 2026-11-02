// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.screengarden.app.R
import com.screengarden.app.ui.components.PlaceholderScreen
import com.screengarden.app.ui.navigation.ScreenGardenDestination

/** Garden tab placeholder. Real content arrives with the garden feature. */
@Composable
fun GardenScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.tab_garden),
        description = stringResource(R.string.garden_placeholder),
        icon = ScreenGardenDestination.Garden.icon,
        modifier = modifier
    )
}
