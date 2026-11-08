// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.insights.ReflectionSuggestion
import com.screengarden.app.domain.insights.WeeklyReflection
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

/** Calm local templates. Focus totals remain known even when usage is not measured. */
@Composable
fun WeeklyReflectionCard(reflection: WeeklyReflection, onHistory: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.reflection_title),
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content)
            )
            Text(
                pluralStringResource(R.plurals.reflection_coverage, reflection.measuredDays, reflection.measuredDays),
                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
            )
            val saved = reflection.minutesBelowGoal
            Text(
                if (saved == null) {
                    stringResource(R.string.reflection_unknown)
                } else {
                    stringResource(
                        when {
                            saved > 0 -> R.string.reflection_saved
                            saved < 0 -> R.string.reflection_over
                            else -> R.string.reflection_on_goal
                        },
                        insightTime(abs(saved))
                    )
                }
            )
            val best = reflection.bestDate
            if (best != null) {
                Text(
                    stringResource(
                        R.string.reflection_best,
                        best.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        reflection.bestScore ?: 0
                    )
                )
            }
            Text(stringResource(R.string.reflection_focus, insightTime(reflection.focusMinutes)))
            Text(
                stringResource(
                    when (reflection.suggestion) {
                        ReflectionSuggestion.BEGIN -> R.string.reflection_begin
                        ReflectionSuggestion.WIND_DOWN -> R.string.reflection_wind_down
                        ReflectionSuggestion.PAUSE -> R.string.reflection_pause
                        ReflectionSuggestion.FOCUS -> R.string.reflection_try_focus
                        ReflectionSuggestion.KEEP_GOING -> R.string.reflection_keep_going
                    }
                )
            )
            TextButton(onHistory) { Text(stringResource(R.string.history_title)) }
        }
    }
}
