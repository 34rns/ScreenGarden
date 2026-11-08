// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.habits.BadgeKind
import com.screengarden.app.ui.theme.ScreenGardenTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Quiet, text-labelled milestones; all controls remain usable with larger text and RTL layouts. */
@Composable
fun HabitsScreen(
    state: HabitsUiState,
    onCompassionDay: (String, Boolean) -> Unit,
    onThreshold: (Int) -> Unit,
    onMonthlyLimit: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    var date by rememberSaveable(state.today) { mutableStateOf(state.today.toString()) }
    var threshold by remember(state.threshold) { mutableStateOf(state.threshold.toFloat()) }
    var limit by remember(state.monthlyLimit) { mutableStateOf(state.monthlyLimit.toFloat()) }
    val selected = runCatching { LocalDay.startOfDay(LocalDate.parse(date)) }.getOrNull()
    val marked = state.snapshot.days.firstOrNull { it.dayStartUtcMillis == selected }?.isCompassionDay == true
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content)
    ) {
        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.habits_back)) }
            Text(
                stringResource(R.string.habits_title),
                Modifier.semantics {
                    heading()
                },
                style = MaterialTheme.typography.headlineLarge.copy(textDirection = TextDirection.Content)
            )
            Text(
                stringResource(R.string.habits_subtitle),
                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content)
            )
            if (state.loading) CircularProgressIndicator()
            if (state.error) Text(stringResource(R.string.habits_load_error), color = MaterialTheme.colorScheme.error)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        pluralStringResource(R.plurals.habits_streak, state.streaks.current, state.streaks.current),
                        style = MaterialTheme.typography.headlineMedium.copy(textDirection = TextDirection.Content)
                    )
                    Text(pluralStringResource(R.plurals.habits_longest, state.streaks.longest, state.streaks.longest))
                    Text(stringResource(R.string.habits_streak_copy))
                }
            }
            Text(
                stringResource(R.string.habits_badges),
                Modifier.semantics {
                    heading()
                },
                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content)
            )
            for (kind in BadgeKind.entries) {
                val earned = state.snapshot.badges.firstOrNull { it.kind == kind }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (earned != null) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    )
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(badgeTitle(kind)),
                            style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content)
                        )
                        Text(stringResource(badgeDescription(kind)))
                        Text(
                            if (earned == null) {
                                stringResource(R.string.habits_badge_waiting)
                            } else {
                                stringResource(
                                    R.string.habits_earned,
                                    Instant.ofEpochMilli(earned.unlockedAtUtcMillis)
                                        .atZone(
                                            ZoneId.systemDefault()
                                        ).toLocalDate().format(
                                            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
                                        )
                                )
                            }
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.habits_rest_title),
                Modifier.semantics {
                    heading()
                },
                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content)
            )
            Text(stringResource(R.string.habits_rest_copy))
            OutlinedTextField(
                date,
                { date = it },
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.habits_date)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { date = state.today.toString() }) { Text(stringResource(R.string.habits_today)) }
                Button(
                    onClick = { onCompassionDay(date, !marked) },
                    enabled =
                    !state.saving && !state.loading && !state.error
                ) {
                    Text(stringResource(if (marked) R.string.habits_unmark else R.string.habits_mark))
                }
            }
            state.message?.let { Text(stringResource(messageText(it)), color = MaterialTheme.colorScheme.primary) }
            val offDays = state.snapshot.days.filter {
                it.isCompassionDay
            }.sortedByDescending { it.dayStartUtcMillis }.take(12)
            if (offDays.isNotEmpty()) {
                Text(stringResource(R.string.habits_marked_dates))
                Text(
                    offDays.joinToString {
                        Instant.ofEpochMilli(
                            it.dayStartUtcMillis
                        ).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    }
                )
            }
            val thresholdLabel = stringResource(R.string.habits_threshold, threshold.toInt())
            Text(thresholdLabel)
            Slider(
                threshold,
                { threshold = it },
                onValueChangeFinished = { onThreshold(threshold.toInt()) },
                modifier = Modifier.semantics { contentDescription = thresholdLabel },
                valueRange = 0f..100f,
                steps = 99,
                enabled = !state.saving && !state.loading && !state.error
            )
            val allowanceLabel = stringResource(R.string.habits_allowance, limit.toInt())
            Text(allowanceLabel)
            Slider(
                limit,
                { limit = it },
                onValueChangeFinished = { onMonthlyLimit(limit.toInt()) },
                modifier = Modifier.semantics { contentDescription = allowanceLabel },
                valueRange = 0f..10f,
                steps = 9,
                enabled = !state.saving && !state.loading && !state.error
            )
        }
    }
}

private fun badgeTitle(kind: BadgeKind): Int = when (kind) {
    BadgeKind.STREAK_7 -> R.string.badge_streak_7
    BadgeKind.STREAK_30 -> R.string.badge_streak_30
    BadgeKind.FOCUS_WEEK_10 -> R.string.badge_focus_week
    BadgeKind.FIRST_BLOOM -> R.string.badge_first_bloom
    BadgeKind.FIRST_RECOVERY -> R.string.badge_first_recovery
}
private fun badgeDescription(kind: BadgeKind): Int = when (kind) {
    BadgeKind.STREAK_7 -> R.string.badge_streak_7_copy
    BadgeKind.STREAK_30 -> R.string.badge_streak_30_copy
    BadgeKind.FOCUS_WEEK_10 -> R.string.badge_focus_week_copy
    BadgeKind.FIRST_BLOOM -> R.string.badge_first_bloom_copy
    BadgeKind.FIRST_RECOVERY -> R.string.badge_first_recovery_copy
}
private fun messageText(message: HabitsMessage): Int = when (message) {
    HabitsMessage.SAVED -> R.string.habits_saved
    HabitsMessage.INVALID_DATE -> R.string.habits_invalid_date
    HabitsMessage.FUTURE_DATE -> R.string.habits_future_date
    HabitsMessage.LIMIT_REACHED -> R.string.habits_limit_reached
    HabitsMessage.SAVE_ERROR -> R.string.habits_save_error
}

@Preview(showBackground = true)
@Composable
private fun HabitsPreview() {
    ScreenGardenTheme { HabitsScreen(HabitsUiState(loading = false), { _, _ -> }, { }, { }, { }) }
}
