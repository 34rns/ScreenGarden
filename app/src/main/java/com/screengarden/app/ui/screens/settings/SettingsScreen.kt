// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.screengarden.app.R

/** Settings entry point; other settings sections arrive in P22. */
@Composable
fun SettingsScreen(
    onAppLimits: () -> Unit,
    modifier: Modifier = Modifier,
    onMilestones: () -> Unit = { },
    environmentContent: @Composable () -> Unit = { }
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text(
                stringResource(R.string.tab_settings),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
            TextButton(onClick = onAppLimits, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.limits_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.limits_settings_copy),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
                    )
                }
            }
            TextButton(onClick = onMilestones, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.habits_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.habits_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
                    )
                }
            }
            Surface(Modifier.fillMaxWidth().padding(top = 24.dp)) { environmentContent() }
        }
    }
}
