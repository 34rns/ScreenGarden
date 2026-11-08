// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.focus.FocusBlockPolicy

/** Permission explanation and policy choices, without Android service or storage access. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusBlockingScreen(
    state: FocusBlockingUiState,
    granted: Boolean,
    onSave: (FocusBlockPolicy) -> Unit,
    onPermission: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var explain by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val editable = !state.loading && !state.saving && !state.error
    val policy = state.policy
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onBack) { Text(stringResource(R.string.blocking_back)) }
                    Text(
                        stringResource(R.string.blocking_title),
                        Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineLarge.copy(textDirection = TextDirection.Content)
                    )
                    BlockingText(R.string.blocking_intro)
                    BlockingText(R.string.blocking_escapes)
                    if (state.loading || state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (state.error) {
                        BlockingText(R.string.blocking_error)
                        TextButton(onRetry) { Text(stringResource(R.string.blocking_retry)) }
                    }
                    BlockingText(if (granted) R.string.blocking_granted else R.string.blocking_not_granted)
                    OutlinedButton({ explain = true }, Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.blocking_review))
                    }
                    val toggleLabel = stringResource(R.string.blocking_enable)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(toggleLabel, Modifier.weight(1f))
                        Switch(
                            policy.enabled,
                            { onSave(policy.copy(enabled = it)) },
                            Modifier.semantics { contentDescription = toggleLabel },
                            enabled = editable && (granted || policy.enabled)
                        )
                    }
                    BlockingText(R.string.blocking_lists)
                    OutlinedTextField(
                        query,
                        { query = it },
                        Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.blocking_search)) },
                        singleLine = true
                    )
                }
            }
            val apps = state.apps.filter {
                it.label.contains(query, true) || it.packageName.contains(query, true)
            }
            if (!state.loading && apps.isEmpty()) item { BlockingText(R.string.blocking_empty) }
            items(apps, key = { it.packageName }) { app ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(app.label, style = MaterialTheme.typography.titleMedium)
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val selected = when (app.packageName) {
                            in policy.allowed -> 2
                            in policy.blocked -> 1
                            else -> 0
                        }
                        listOf(R.string.blocking_normal, R.string.blocking_block, R.string.blocking_allow)
                            .forEachIndexed { index, label ->
                                val text = stringResource(label)
                                val description = stringResource(R.string.blocking_choice, app.label, text)
                                FilterChip(
                                    selected == index,
                                    {
                                        onSave(
                                            policy.copy(
                                                blocked = (policy.blocked - app.packageName) +
                                                    if (index == 1) setOf(app.packageName) else emptySet(),
                                                allowed = (policy.allowed - app.packageName) +
                                                    if (index == 2) setOf(app.packageName) else emptySet()
                                            )
                                        )
                                    },
                                    { Text(text) },
                                    Modifier.heightIn(min = 48.dp).semantics { contentDescription = description },
                                    enabled = editable &&
                                        when (index) {
                                            1 -> app.packageName in policy.blocked || policy.blocked.size < 256
                                            2 -> app.packageName in policy.allowed || policy.allowed.size < 256
                                            else -> true
                                        }
                                )
                            }
                    }
                }
            }
            item { BlockingText(R.string.blocking_limits) }
        }
    }
    if (explain) {
        AlertDialog(
            { explain = false },
            title = { Text(stringResource(R.string.blocking_review)) },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    BlockingText(R.string.blocking_permission_description)
                }
            },
            confirmButton = {
                TextButton({
                    explain = false
                    onPermission()
                }) {
                    Text(stringResource(R.string.blocking_open_settings))
                }
            },
            dismissButton = {
                TextButton({ explain = false }) { Text(stringResource(R.string.settings_cancel)) }
            }
        )
    }
}

@Composable
private fun BlockingText(label: Int) {
    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content))
}
