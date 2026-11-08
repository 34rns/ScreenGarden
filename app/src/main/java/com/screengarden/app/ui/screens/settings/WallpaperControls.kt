// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import com.screengarden.app.R

/** Android owns preview and applying; existing theme/hemisphere/low-power choices customize the scene. */
@Composable
fun WallpaperControls(onPreview: () -> Boolean, modifier: Modifier = Modifier) {
    var failed by rememberSaveable { mutableStateOf(false) }
    Column(modifier) {
        Text(
            stringResource(R.string.wallpaper_copy),
            style = MaterialTheme.typography.bodyMedium.copy(
                textDirection = TextDirection.Content
            )
        )
        OutlinedButton({ failed = !onPreview() }, Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.wallpaper_preview))
        }
        if (failed) Text(stringResource(R.string.wallpaper_unavailable))
    }
}
