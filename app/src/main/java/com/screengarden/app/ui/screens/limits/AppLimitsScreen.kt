// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.limits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.limits.AppLimitRule
import com.screengarden.app.domain.limits.LaunchableApp
import com.screengarden.app.ui.theme.ScreenGardenTheme

/** Searchable local app inventory. Editing and pausing are deliberate, reversible actions. */
@Composable
fun AppLimitsScreen(
    state: AppLimitsUiState,
    onSave: (String, Int, Boolean) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var minutesText by rememberSaveable { mutableStateOf("30") }
    val focusManager = LocalFocusManager.current
    val limits = state.limits.associateBy { it.packageName }
    val visible = state.apps.filter {
        it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
    }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.limits_back)) }
                    Text(
                        stringResource(R.string.limits_title),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        stringResource(R.string.limits_copy),
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
                    )
                }
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.limits_search)) },
                        modifier = Modifier.fillMaxWidth().testTag("limits_search"),
                        singleLine = true
                    )
                }
                if (state.loading || state.saving) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                if (state.error) {
                    item {
                        Text(
                            stringResource(R.string.limits_error),
                            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content),
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.limits_retry)) }
                    }
                }
                if (!state.loading && visible.isEmpty()) {
                    item {
                        Text(
                            stringResource(
                                if (state.apps.isEmpty()) R.string.limits_no_apps else R.string.limits_no_match
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
                        )
                        if (state.apps.isEmpty()) {
                            TextButton(onClick = onRetry) { Text(stringResource(R.string.limits_retry)) }
                        }
                    }
                }
                items(visible, key = { it.packageName }) { app ->
                    val limit = limits[app.packageName]
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            app.label,
                            style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content)
                        )
                        Text(
                            app.packageName,
                            style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    selectedPackage = app.packageName
                                    minutesText = (limit?.dailyLimitMinutes ?: 30).toString()
                                },
                                enabled = !state.saving,
                                modifier = Modifier.weight(1f).testTag("edit_${app.packageName}")
                            ) {
                                Text(
                                    if (limit == null) {
                                        stringResource(R.string.limits_set)
                                    } else {
                                        stringResource(R.string.limits_duration, limit.dailyLimitMinutes)
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        textDirection = TextDirection.Content
                                    )
                                )
                            }
                            val toggleLabel = stringResource(R.string.limits_toggle, app.label)
                            Switch(
                                checked = limit?.isEnabled == true,
                                enabled = limit != null && !state.saving,
                                onCheckedChange = { enabled ->
                                    if (limit != null) onSave(app.packageName, limit.dailyLimitMinutes, enabled)
                                },
                                modifier = Modifier.semantics { contentDescription = toggleLabel }
                                    .testTag("toggle_${app.packageName}")
                            )
                        }
                        if (limit?.isEnabled == false) {
                            Text(
                                stringResource(R.string.limits_paused),
                                style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Content),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedPackage == app.packageName) {
                            val minutes = minutesText.toIntOrNull()
                            val valid = minutes != null && minutes in 1..1440
                            OutlinedTextField(
                                value = minutesText,
                                onValueChange = { minutesText = it },
                                label = { Text(stringResource(R.string.limits_minutes)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                isError = !valid,
                                supportingText = {
                                    Text(
                                        stringResource(R.string.limits_range),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            textDirection = TextDirection.Content
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("limits_minutes")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    enabled = valid && !state.saving,
                                    onClick = {
                                        onSave(app.packageName, requireNotNull(minutes), true)
                                        focusManager.clearFocus()
                                        selectedPackage = null
                                    },
                                    modifier = Modifier.testTag("limits_save")
                                ) { Text(stringResource(R.string.limits_save)) }
                                TextButton(onClick = {
                                    focusManager.clearFocus()
                                    selectedPackage = null
                                }) { Text(stringResource(R.string.limits_cancel)) }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppLimitsPreview() {
    ScreenGardenTheme {
        AppLimitsScreen(
            AppLimitsUiState(
                apps = listOf(LaunchableApp("demo.reading", "Reading"), LaunchableApp("demo.video", "Videos")),
                limits = listOf(AppLimitRule("demo.video", 30)),
                loading = false
            ),
            { _, _, _ -> },
            { },
            { }
        )
    }
}
