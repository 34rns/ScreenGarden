// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.focus.FocusStatus
import com.screengarden.app.domain.focus.FocusTag
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.ui.screens.garden.LocalPlantRenderer
import com.screengarden.app.ui.screens.garden.PlantPalette
import com.screengarden.app.ui.screens.garden.PlantVisuals
import com.screengarden.app.ui.theme.ScreenGardenTheme

/** Focus setup, countdown and results; service work stays outside the screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(
    state: FocusUiState,
    onStart: (Int, FocusTag) -> Unit,
    onStop: () -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onDismissResult: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    var custom by rememberSaveable { mutableStateOf(false) }
    var customText by rememberSaveable { mutableStateOf("25") }
    var tagName by rememberSaveable { mutableStateOf(FocusTag.STUDY.name) }
    val configured = if (custom) customText.toIntOrNull() else minutes
    val active = state.active
    val result = state.result
    val view = LocalView.current
    DisposableEffect(view, state.keepScreenOn, active != null) {
        view.keepScreenOn = state.keepScreenOn && active != null
        onDispose { view.keepScreenOn = false }
    }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FocusText(
                    stringResource(R.string.tab_focus),
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge
                )
                if (state.loading || state.pending) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (state.error) {
                    FocusText(stringResource(R.string.focus_action_error), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { FocusText(stringResource(R.string.focus_retry)) }
                }
                when {
                    active != null -> {
                        val progress = (active.actualDurationMs.toFloat() / active.plannedDurationMs).coerceIn(0f, 1f)
                        FocusSeed(progress)
                        val seconds = (active.remainingDurationMs + 999) / 1000
                        FocusText(
                            stringResource(R.string.focus_countdown, seconds / 60, seconds % 60),
                            modifier = Modifier.testTag("focus_countdown"),
                            style = MaterialTheme.typography.displayLarge
                        )
                        FocusText(stringResource(tagLabel(active.tag)), style = MaterialTheme.typography.titleMedium)
                        FocusText(stringResource(R.string.focus_running_copy), textAlign = TextAlign.Center)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        OutlinedButton(
                            onClick = onStop,
                            enabled = !state.pending,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            FocusText(stringResource(R.string.focus_stop))
                        }
                    }
                    result != null -> {
                        val completed = result.status == FocusStatus.COMPLETED
                        FocusSeed(if (completed) 1f else 0.35f)
                        FocusText(
                            stringResource(if (completed) R.string.focus_complete_title else R.string.focus_rest_title),
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        FocusText(
                            stringResource(
                                when (result.status) {
                                    FocusStatus.COMPLETED -> R.string.focus_complete_copy
                                    FocusStatus.FAILED_SOFT -> R.string.focus_soft_copy
                                    else -> R.string.focus_cancel_copy
                                }
                            ),
                            textAlign = TextAlign.Center
                        )
                        FocusText(stringResource(R.string.focus_actual_minutes, result.actualDurationMs / 60_000))
                        if (completed) {
                            FocusText(
                                stringResource(R.string.focus_reward_summary),
                                textAlign = TextAlign.Center
                            )
                        }
                        Button(onClick = onDismissResult, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            FocusText(stringResource(R.string.focus_another))
                        }
                    }
                    else -> {
                        FocusSeed(0f)
                        FocusText(
                            stringResource(R.string.focus_setup_title),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        FocusText(stringResource(R.string.focus_setup_copy), textAlign = TextAlign.Center)
                        FocusText(
                            stringResource(R.string.focus_duration),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (choice in listOf(15, 25, 45, 60)) {
                                FilterChip(
                                    selected = !custom && minutes == choice,
                                    onClick = {
                                        custom = false
                                        minutes = choice
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    label = { FocusText(stringResource(R.string.focus_duration_minutes, choice)) }
                                )
                            }
                            FilterChip(
                                selected = custom,
                                onClick = { custom = true },
                                modifier = Modifier.heightIn(min = 48.dp),
                                label = {
                                    FocusText(stringResource(R.string.focus_custom))
                                }
                            )
                        }
                        if (custom) {
                            OutlinedTextField(
                                value = customText,
                                onValueChange = { customText = it.take(3) },
                                label = { FocusText(stringResource(R.string.focus_custom_label)) },
                                supportingText = { FocusText(stringResource(R.string.focus_duration_range)) },
                                isError = configured == null || configured !in 1..240,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("focus_custom_minutes")
                            )
                        }
                        FocusText(
                            stringResource(R.string.focus_tag),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (tag in FocusTag.entries) {
                                FilterChip(
                                    selected = tagName == tag.name,
                                    onClick = { tagName = tag.name },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    label = { FocusText(stringResource(tagLabel(tag))) }
                                )
                            }
                        }
                        Button(
                            onClick = { configured?.let { onStart(it, FocusTag.valueOf(tagName)) } },
                            enabled = !state.loading && !state.pending && configured != null && configured in 1..240,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        ) {
                            FocusText(stringResource(R.string.focus_plant_seed))
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = 48.dp).toggleable(
                            state.keepScreenOn,
                            enabled = !state.pending,
                            role = Role.Checkbox,
                            onValueChange = onKeepScreenOn
                        )
                ) {
                    Checkbox(checked = state.keepScreenOn, onCheckedChange = null)
                    FocusText(stringResource(R.string.focus_keep_screen_on))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FocusText(
                        pluralStringResource(R.plurals.focus_sessions_today, state.sessionsToday, state.sessionsToday),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    FocusText(
                        stringResource(R.string.focus_week_minutes, state.minutesThisWeek),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusSeed(progress: Float) {
    val colors = MaterialTheme.colorScheme
    val renderer = LocalPlantRenderer.current
    val palette = PlantPalette(
        colors.primary,
        colors.outline,
        colors.secondary,
        colors.tertiary,
        colors.tertiaryContainer,
        colors.onSecondaryContainer
    )
    val description = stringResource(R.string.focus_seed_description)
    Canvas(modifier = Modifier.size(160.dp).semantics { contentDescription = description }) {
        renderer.draw(
            this,
            Plant("focus-visual", "flower", growthPoints = (progress * 45).toInt()),
            palette,
            PlantVisuals(progress, ((progress - 0.5f) * 2).coerceIn(0f, 1f), 0f)
        )
    }
}

@Composable
private fun FocusText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        style = style.copy(textDirection = TextDirection.Content)
    )
}

private fun tagLabel(tag: FocusTag): Int = when (tag) {
    FocusTag.STUDY -> R.string.focus_tag_study
    FocusTag.WORK -> R.string.focus_tag_work
    FocusTag.READING -> R.string.focus_tag_reading
    FocusTag.OTHER -> R.string.focus_tag_other
}

@Preview
@Composable
private fun FocusPreview() {
    ScreenGardenTheme { FocusScreen(FocusUiState(loading = false), { _, _ -> }, { }, { }, { }, { }) }
}
