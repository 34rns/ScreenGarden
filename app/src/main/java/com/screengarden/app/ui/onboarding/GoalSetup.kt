// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.model.OnboardingChoices
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The goal step: screen-time goal, pickup goal and the bedtime window (16.7).
 *
 * Each slider is discrete (fixed steps) so the value is always a round number the
 * user can repeat later in Settings, and the current value is shown as text next to
 * the label - colour alone never carries meaning, and a screen reader reads the value
 * from the slider's own description.
 */
@Composable
fun GoalSetup(choices: OnboardingChoices, onChoicesChange: (OnboardingChoices) -> Unit, modifier: Modifier = Modifier) {
    val dailyGoalRange = sliderRange(OnboardingChoices.DAILY_GOAL_RANGE_MINUTES)
    val pickupRange = sliderRange(OnboardingChoices.PICKUP_GOAL_RANGE)
    val bedtimeStartRange = sliderRange(OnboardingChoices.BEDTIME_START_RANGE_MINUTES)
    val bedtimeEndRange = sliderRange(OnboardingChoices.BEDTIME_END_RANGE_MINUTES)
    val locale = LocalConfiguration.current.locales[0]
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        GoalSlider(
            label = stringResource(R.string.onboarding_goal_screen_time),
            valueText = durationText(choices.dailyGoalMinutes),
            value = choices.dailyGoalMinutes.toFloat(),
            range = dailyGoalRange,
            stepMinutes = OnboardingChoices.DAILY_GOAL_STEP_MINUTES,
            onValueChange = { minutes ->
                onChoicesChange(choices.copy(dailyGoalMs = minutes.toLong() * OnboardingChoices.MILLIS_PER_MINUTE))
            }
        )
        GoalSlider(
            label = stringResource(R.string.onboarding_goal_pickups),
            valueText = NumberFormat.getIntegerInstance(locale).format(choices.pickupGoal),
            value = choices.pickupGoal.toFloat(),
            range = pickupRange,
            stepMinutes = OnboardingChoices.PICKUP_GOAL_STEP,
            onValueChange = { value -> onChoicesChange(choices.copy(pickupGoal = value.toInt())) }
        )
        GoalSlider(
            label = stringResource(R.string.onboarding_goal_bedtime_start),
            valueText = LocalTime.ofSecondOfDay(choices.bedtimeStartMinutes * 60L).format(time),
            value = choices.bedtimeStartMinutes.toFloat(),
            range = bedtimeStartRange,
            stepMinutes = OnboardingChoices.BEDTIME_STEP_MINUTES,
            onValueChange = { value -> onChoicesChange(choices.copy(bedtimeStartMinutes = value.toInt())) }
        )
        GoalSlider(
            label = stringResource(R.string.onboarding_goal_bedtime_end),
            valueText = LocalTime.ofSecondOfDay(choices.bedtimeEndMinutes * 60L).format(time),
            value = choices.bedtimeEndMinutes.toFloat(),
            range = bedtimeEndRange,
            stepMinutes = OnboardingChoices.BEDTIME_STEP_MINUTES,
            onValueChange = { value -> onChoicesChange(choices.copy(bedtimeEndMinutes = value.toInt())) }
        )
    }
}

/** Maps a whole-minute range from the model onto the floats a Slider needs. */
private fun sliderRange(minutes: IntRange): ClosedFloatingPointRange<Float> =
    minutes.first.toFloat()..minutes.last.toFloat()

/**
 * One labelled slider. The description combines the label and the formatted value so
 * a screen reader announces "Pickups per day, 80" instead of a bare value.
 */
@Composable
private fun GoalSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    stepMinutes: Int,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Discrete steps between the endpoints, which is what Slider.steps expects.
    val steps = ((range.endInclusive - range.start) / stepMinutes).toInt() - 1
    val description = stringResource(R.string.onboarding_slider_description, label, valueText)
    Column(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = description }
        )
    }
}

/** "2 h 30 m", or "45 min" below an hour. */
@Composable
private fun durationText(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (hours == 0) {
        stringResource(R.string.onboarding_duration_minutes, remainder)
    } else {
        stringResource(R.string.onboarding_duration_hours_minutes, hours, remainder)
    }
}
