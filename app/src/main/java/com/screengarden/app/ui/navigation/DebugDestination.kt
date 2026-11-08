// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.navigation

import androidx.compose.runtime.Composable
import com.screengarden.app.domain.model.ThemePreference

/**
 * A destination that exists only in debug builds (P05b).
 *
 * HARD RULE H5 keeps the app to a single Activity, so tooling screens are added as
 * extra Compose destinations instead of new Activities. This type is the seam: the
 * debug source set builds one, the release source set never does, and the shipped
 * UI therefore cannot navigate to a diagnostic screen.
 *
 * [content] is a plain composable lambda so main code needs no reference to the
 * debug screen itself - which is also what keeps that class out of a release APK.
 */
class DebugDestination(
    val route: String,
    val selectedTab: ScreenGardenDestination = ScreenGardenDestination.START,
    val themeOverride: ThemePreference? = null,
    val content: @Composable () -> Unit
)
