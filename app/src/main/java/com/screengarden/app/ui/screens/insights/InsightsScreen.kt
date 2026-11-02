// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.insights

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.screengarden.app.R
import com.screengarden.app.ui.components.PlaceholderScreen
import com.screengarden.app.ui.navigation.ScreenGardenDestination

/** Insights tab placeholder. Real content arrives with the usage stats feature. */
@Composable
fun InsightsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.tab_insights),
        description = stringResource(R.string.insights_placeholder),
        icon = ScreenGardenDestination.Insights.icon,
        modifier = modifier
    )
}
