// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.focus

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.screengarden.app.R
import com.screengarden.app.ui.components.PlaceholderScreen
import com.screengarden.app.ui.navigation.ScreenGardenDestination

/** Focus tab placeholder. Real content arrives with the focus session feature. */
@Composable
fun FocusScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.tab_focus),
        description = stringResource(R.string.focus_placeholder),
        icon = ScreenGardenDestination.Focus.icon,
        modifier = modifier
    )
}
