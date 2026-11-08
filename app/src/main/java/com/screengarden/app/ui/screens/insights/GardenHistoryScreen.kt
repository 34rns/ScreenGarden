// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.insights.HistoryPeriod
import com.screengarden.app.ui.screens.garden.GardenPlantArt
import com.screengarden.app.ui.screens.garden.plantNameResource
import com.screengarden.app.ui.screens.garden.plantStageResource
import com.screengarden.app.ui.theme.ScreenGardenTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Actual stored plant frames, with explicit coverage and no imaginary backfill. */
@Composable
fun GardenHistoryScreen(
    state: GardenHistoryUiState,
    onPeriod: (HistoryPeriod) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val capturedFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.bodyLarge.copy(
            textDirection = TextDirection.Content
        )
    ) {
        LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                TextButton(onBack) { Text(stringResource(R.string.history_back)) }
                Text(
                    stringResource(R.string.history_title),
                    Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge.copy(textDirection = TextDirection.Content)
                )
            }
            item {
                Text(stringResource(R.string.history_copy))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryPeriod.entries.forEach { period ->
                        FilterChip(state.period == period, { onPeriod(period) }, label = {
                            Text(
                                stringResource(
                                    if (period == HistoryPeriod.WEEK) {
                                        R.string.history_weeks
                                    } else {
                                        R.string.history_months
                                    }
                                )
                            )
                        })
                    }
                }
            }
            if (state.loading) item { CircularProgressIndicator() }
            if (state.error) {
                item {
                    Text(stringResource(R.string.history_error), color = MaterialTheme.colorScheme.error)
                    TextButton(onRefresh) { Text(stringResource(R.string.insights_refresh)) }
                }
            }
            if (state.unreadableFrames > 0) {
                item {
                    Text(
                        pluralStringResource(
                            R.plurals.history_unreadable,
                            state.unreadableFrames,
                            state.unreadableFrames
                        )
                    )
                }
            }
            if (!state.loading && state.entries.isEmpty()) item { Text(stringResource(R.string.history_empty)) }
            items(state.entries, key = { it.periodStart.toEpochDay() }) { entry ->
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(
                                if (state.period == HistoryPeriod.WEEK) {
                                    R.string.history_week
                                } else {
                                    R.string.history_month
                                },
                                entry.periodStart.format(format)
                            ),
                            Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content)
                        )
                        Text(
                            pluralStringResource(
                                R.plurals.history_recorded_days,
                                entry.recordedDays,
                                entry.recordedDays,
                                entry.frame.score
                            )
                        )
                        entry.frame.plants.chunked(3).forEach { plants ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                plants.forEach { plant ->
                                    Column(Modifier.weight(1f)) {
                                        GardenPlantArt(plant, Modifier.fillMaxWidth().height(110.dp))
                                        Text(
                                            stringResource(
                                                R.string.history_plant,
                                                stringResource(plantNameResource(plant.species)),
                                                stringResource(plantStageResource(plant.stage)),
                                                plant.health
                                            ),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                textDirection = TextDirection.Content
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        if (entry.frame.plants.isEmpty()) Text(stringResource(R.string.history_empty_plot))
                        Text(
                            stringResource(
                                R.string.history_captured,
                                Instant.ofEpochMilli(entry.frame.capturedAtUtcMillis)
                                    .atZone(ZoneId.systemDefault()).format(capturedFormat)
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GardenHistoryPreview() {
    ScreenGardenTheme { GardenHistoryScreen(GardenHistoryUiState(loading = false), {}, {}, {}) }
}
