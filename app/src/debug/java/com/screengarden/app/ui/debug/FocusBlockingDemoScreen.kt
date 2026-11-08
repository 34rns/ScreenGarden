// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.screengarden.app.domain.focus.FocusBlockPolicy
import com.screengarden.app.domain.limits.LaunchableApp
import com.screengarden.app.ui.screens.focus.FocusBlockingScreen
import com.screengarden.app.ui.screens.focus.FocusBlockingUiState

/** Fake presentation only: no database, DataStore, Android grant or accessibility service. */
@Composable
fun FocusBlockingDemoScreen(onBack: () -> Unit = { }) {
    var granted by remember { mutableStateOf(false) }
    var state by remember {
        mutableStateOf(
            FocusBlockingUiState(
                policy = FocusBlockPolicy(blocked = setOf("demo.video"), allowed = setOf("demo.notes")),
                apps = listOf(
                    LaunchableApp("demo.reader", "Reading"),
                    LaunchableApp("demo.notes", "Notes"),
                    LaunchableApp("demo.video", "Videos")
                ),
                loading = false
            )
        )
    }
    FocusBlockingScreen(state, granted, { state = state.copy(policy = it) }, { granted = true }, {}, onBack)
}
