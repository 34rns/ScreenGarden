// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.screengarden.app.R

/** Fake-only notification preview. This source set cannot reach a release build. */
@Composable
fun NudgeDemoScreen(onShow: () -> Boolean, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf<Boolean?>(null) }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(stringResource(R.string.debug_nudge_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.debug_nudge_copy), modifier = Modifier.padding(vertical = 16.dp))
            Button(onClick = { shown = onShow() }, modifier = Modifier.testTag("nudge_demo_show")) {
                Text(stringResource(R.string.debug_nudge_show))
            }
            shown?.let { success ->
                Text(stringResource(if (success) R.string.debug_nudge_shown else R.string.debug_nudge_blocked))
            }
        }
    }
}
