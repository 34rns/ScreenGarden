// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Static Canvas charts with separate scales and a readable-value companion in the screen. */
@Composable
fun InsightsCharts(state: InsightsUiState, modifier: Modifier = Modifier) {
    val days = state.summary.days
    val bar = MaterialTheme.colorScheme.primary
    val score = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val goal = MaterialTheme.colorScheme.onSurfaceVariant
    val goalTime = insightTime(state.goalMinutes.toLong())
    val barDescription = pluralStringResource(
        R.plurals.insights_bar_description,
        state.summary.measuredDays,
        state.summary.measuredDays,
        goalTime
    )
    val scoreDescription = stringResource(R.string.insights_score_description)
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.insights_screen_chart),
            style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content)
        )
        Text(
            stringResource(R.string.insights_goal_legend, goalTime),
            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
        )
        Canvas(Modifier.fillMaxWidth().height(172.dp).semantics { contentDescription = barDescription }) {
            if (days.isEmpty()) return@Canvas
            val maximum = maxOf(state.goalMinutes, days.maxOf { it.minutes ?: 0 }, 1).toFloat() * 1.1f
            val step = size.width / days.size
            val base = size.height - 2.dp.toPx()
            drawLine(grid, Offset(0f, base), Offset(size.width, base), 1.dp.toPx())
            days.forEachIndexed { index, day ->
                day.minutes?.let { value ->
                    val height = value / maximum * base
                    val position = if (rtl) days.lastIndex - index else index
                    drawRect(bar, Offset(position * step + step * 0.15f, base - height), Size(step * 0.7f, height))
                }
            }
            val goalY = base - state.goalMinutes / maximum * base
            drawLine(
                goal,
                Offset(0f, goalY),
                Offset(size.width, goalY),
                2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.firstOrNull()?.let {
                Text(
                    it.date.format(dateFormat),
                    style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Content)
                )
            }
            days.lastOrNull()?.let {
                Text(
                    it.date.format(dateFormat),
                    style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Content)
                )
            }
        }
        Text(
            stringResource(R.string.insights_score_chart),
            style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content)
        )
        Text(
            stringResource(R.string.insights_score_legend),
            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
        )
        Canvas(Modifier.fillMaxWidth().height(128.dp).semantics { contentDescription = scoreDescription }) {
            drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            drawLine(grid, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx())
            var previous: Offset? = null
            days.forEachIndexed { index, day ->
                val point = day.score?.let {
                    Offset(
                        ((if (rtl) days.lastIndex - index else index) + 0.5f) * size.width / days.size.coerceAtLeast(1),
                        size.height * (1f - it / 100f)
                    )
                }
                if (point != null) {
                    previous?.let { drawLine(score, it, point, 3.dp.toPx()) }
                    drawCircle(score, 3.dp.toPx(), point)
                }
                previous = point
            }
        }
    }
}
