// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.insights.InsightsPeriod
import com.screengarden.app.ui.theme.ScreenGardenTheme
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Saved totals remain available without permission; missing measurements have explicit states. */
@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onPeriod: (InsightsPeriod) -> Unit,
    onRefresh: () -> Unit,
    onGrantUsage: () -> Unit,
    modifier: Modifier = Modifier,
    onHistory: () -> Unit = {}
) {
    var valuesVisible by rememberSaveable(state.period) { mutableStateOf(false) }
    val unknown = stringResource(R.string.insights_unknown)
    val locale = LocalConfiguration.current.locales[0]
    val number = NumberFormat.getIntegerInstance(locale)
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    CompositionLocalProvider(
        LocalTextStyle provides
            MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content)
    ) {
        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.tab_insights),
                    Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge.copy(textDirection = TextDirection.Content)
                )
                TextButton(onRefresh, enabled = !state.refreshing) { Text(stringResource(R.string.insights_refresh)) }
            }
            Text(stringResource(R.string.insights_subtitle))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InsightsPeriod.entries.forEach { period ->
                    FilterChip(state.period == period, { onPeriod(period) }, label = {
                        Text(
                            stringResource(
                                when (period) {
                                    InsightsPeriod.DAY -> R.string.insights_day
                                    InsightsPeriod.WEEK -> R.string.insights_week
                                    InsightsPeriod.MONTH -> R.string.insights_month
                                }
                            )
                        )
                    })
                }
            }
            if (state.loading || state.refreshing) CircularProgressIndicator()
            if (state.error) Text(stringResource(R.string.insights_read_error), color = MaterialTheme.colorScheme.error)
            if (state.refreshError) {
                Text(
                    stringResource(R.string.insights_refresh_error),
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (!state.hasUsageAccess && !state.loading && !state.refreshing) {
                Text(stringResource(R.string.insights_no_access))
                TextButton(onGrantUsage) { Text(stringResource(R.string.insights_enable)) }
            }
            val summary = state.summary
            if (summary.days.isNotEmpty()) {
                Text(
                    stringResource(
                        R.string.insights_range,
                        summary.days.first().date.format(dateFormat),
                        summary.days.last().date.format(dateFormat)
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content)
                )
                Text(
                    pluralStringResource(
                        R.plurals.insights_coverage,
                        summary.days.size,
                        summary.measuredDays,
                        summary.days.size
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
                )
            }
            Text(
                stringResource(R.string.insights_total),
                style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content)
            )
            Text(
                summary.totalMinutes?.let { insightTime(it) } ?: unknown,
                style = MaterialTheme.typography.headlineLarge.copy(textDirection = TextDirection.Content)
            )
            Text(
                stringResource(
                    R.string.insights_counts,
                    summary.pickups?.let { number.format(it) } ?: unknown,
                    summary.averageScore?.let { stringResource(R.string.insights_score_value, it) } ?: unknown
                )
            )
            if (!state.loading && summary.measuredDays == 0) {
                Text(
                    stringResource(R.string.insights_empty),
                    style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content)
                )
                Text(stringResource(R.string.insights_empty_copy))
            }
            WeeklyReflectionCard(state.reflection, onHistory)
            InsightsCharts(state)
            TextButton({ valuesVisible = !valuesVisible }) {
                Text(
                    stringResource(if (valuesVisible) R.string.insights_hide_values else R.string.insights_show_values)
                )
            }
            if (valuesVisible) {
                summary.days.forEach { day ->
                    Text(
                        stringResource(
                            R.string.insights_daily_value,
                            day.date.format(dateFormat),
                            if (day.isCompassionDay) {
                                stringResource(R.string.insights_off)
                            } else {
                                day.minutes?.let { insightTime(it.toLong()) } ?: unknown
                            },
                            day.score?.let { number.format(it) } ?: unknown,
                            day.pickups?.let { number.format(it) } ?: unknown
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
                    )
                }
            }
            Text(
                stringResource(R.string.insights_top_apps),
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content)
            )
            if (summary.topApps.isEmpty()) Text(stringResource(R.string.insights_no_apps))
            summary.topApps.forEach { app ->
                Text(stringResource(R.string.insights_app_value, app.label, insightTime(app.usageMillis / 60_000)))
            }
            HourTiles(state)
            Text(
                stringResource(R.string.insights_retention),
                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
            )
        }
    }
}

@Composable
internal fun insightTime(minutes: Long): String = stringResource(R.string.insights_time, minutes / 60, minutes % 60)

@Composable
private fun HourTiles(state: InsightsUiState) {
    val eligible = state.summary.days.filter { it.minutes != null }.map { it.dayStartUtcMillis }.toSet()
    val days = state.hours.filter { it.dayStartUtcMillis in eligible && it.usageMillis.size == 24 }
    Text(
        stringResource(R.string.insights_hours),
        Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content)
    )
    Text(
        stringResource(R.string.insights_hours_copy),
        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
    )
    if (days.isEmpty()) {
        Text(stringResource(R.string.insights_hours_empty))
        return
    }
    val bins = List(24) { hour -> days.sumOf { it.usageMillis[hour].coerceAtLeast(0) } / 60_000 }
    val peak = bins.indices.maxBy { bins[it] }
    val timeFormat = DateTimeFormatter.ofLocalizedTime(
        FormatStyle.SHORT
    ).withLocale(LocalConfiguration.current.locales[0])
    if (bins[peak] == 0L) {
        Text(stringResource(R.string.insights_hours_zero))
    } else {
        Text(
            stringResource(
                R.string.insights_peak,
                LocalTime.of(peak, 0).format(timeFormat),
                insightTime(bins[peak]),
                pluralStringResource(R.plurals.insights_recent_days, days.size, days.size)
            )
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(6) { column ->
                    val hour = row * 6 + column
                    val clock = LocalTime.of(
                        hour,
                        0
                    ).format(DateTimeFormatter.ofPattern("HH:mm", LocalConfiguration.current.locales[0]))
                    val description = pluralStringResource(
                        R.plurals.insights_hour_value,
                        bins[hour].coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                        clock,
                        bins[hour]
                    )
                    val intensity = bins[hour].toFloat() / bins.maxOrNull()!!.coerceAtLeast(1)
                    Column(
                        Modifier.weight(1f).background(
                            MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.3f + intensity * 0.7f
                            ),
                            RoundedCornerShape(8.dp)
                        ).padding(vertical = 10.dp)
                            .semantics(mergeDescendants = true) { contentDescription = description },
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text(clock, style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Ltr))
                        Text(
                            stringResource(R.string.insights_hour_minutes, bins[hour]),
                            style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Content)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InsightsPreview() {
    ScreenGardenTheme { InsightsScreen(InsightsUiState(loading = false), {}, {}, {}) }
}
