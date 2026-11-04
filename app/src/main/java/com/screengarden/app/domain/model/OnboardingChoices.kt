// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * The four answers the first-run flow collects (AGENT_MEMORY 16.7).
 *
 * Times are kept as minutes after local midnight rather than as strings: a slider
 * naturally produces a number, and "HH:mm" is only needed at the storage boundary.
 * The ranges live here - not in the sliders - so the completion path, the UI and the
 * tests cannot disagree about what a legal value is, and an out-of-range stored
 * value is clamped in exactly one place ([fromStored]).
 *
 * This is a UI model, which is why it sits in domain/model; the defaults mirror
 * [com.screengarden.app.data.settings.SettingsDefaults] but are only the sliders'
 * starting point, because the flow is always seeded from what is actually stored.
 */
data class OnboardingChoices(
    val dailyGoalMs: Long = DEFAULT_DAILY_GOAL_MS,
    val pickupGoal: Int = DEFAULT_PICKUP_GOAL,
    val bedtimeStartMinutes: Int = DEFAULT_BEDTIME_START_MINUTES,
    val bedtimeEndMinutes: Int = DEFAULT_BEDTIME_END_MINUTES
) {

    /** Slider-friendly form of [dailyGoalMs]; whole minutes, rounded down. */
    val dailyGoalMinutes: Int get() = (dailyGoalMs / MILLIS_PER_MINUTE).toInt()

    /** Bedtime start as HH:mm, ready for SettingsRepository. */
    val bedtimeStartText: String get() = textOf(bedtimeStartMinutes)

    /** Bedtime end as HH:mm, ready for SettingsRepository. */
    val bedtimeEndText: String get() = textOf(bedtimeEndMinutes)

    companion object {

        const val MILLIS_PER_MINUTE: Long = 60_000L

        /** Screen-time goal slider: one hour to eight hours, in quarter hours. */
        val DAILY_GOAL_RANGE_MINUTES: IntRange = 60..480
        const val DAILY_GOAL_STEP_MINUTES: Int = 15

        /** Pickup goal slider: ten to two hundred, in fives. */
        val PICKUP_GOAL_RANGE: IntRange = 10..200
        const val PICKUP_GOAL_STEP: Int = 5

        /**
         * Bedtime window the sliders cover: start 20:00-23:30, end 05:00-09:00.
         * Deliberately not wrapping past midnight: a wrapping slider is hard to use
         * with a screen reader, and the product needs a calm default, not every case.
         * Anything outside these ranges is clamped when read back.
         */
        val BEDTIME_START_RANGE_MINUTES: IntRange = 20 * 60..23 * 60 + 30
        val BEDTIME_END_RANGE_MINUTES: IntRange = 5 * 60..9 * 60
        const val BEDTIME_STEP_MINUTES: Int = 30

        /** Mirrors SettingsDefaults: three hours, eighty pickups, 22:30-07:00. */
        const val DEFAULT_DAILY_GOAL_MS: Long = 3 * 60 * 60 * 1000L
        const val DEFAULT_PICKUP_GOAL: Int = 80
        const val DEFAULT_BEDTIME_START_MINUTES: Int = 22 * 60 + 30
        const val DEFAULT_BEDTIME_END_MINUTES: Int = 7 * 60

        private val STORED_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        /**
         * Builds the flow's starting state from stored settings.
         *
         * A value that no longer fits a slider (an older build allowed a wider range,
         * or the file was edited) is clamped instead of crashing, and an unparseable
         * time falls back to the documented default.
         */
        fun fromStored(
            dailyGoalMs: Long,
            pickupGoal: Int,
            bedtimeStart: String,
            bedtimeEnd: String
        ): OnboardingChoices = OnboardingChoices(
            dailyGoalMs = dailyGoalMs.coerceIn(
                DAILY_GOAL_RANGE_MINUTES.first * MILLIS_PER_MINUTE,
                DAILY_GOAL_RANGE_MINUTES.last * MILLIS_PER_MINUTE
            ),
            pickupGoal = pickupGoal.coerceIn(PICKUP_GOAL_RANGE),
            bedtimeStartMinutes = parseMinutes(bedtimeStart)
                ?.coerceIn(BEDTIME_START_RANGE_MINUTES)
                ?: DEFAULT_BEDTIME_START_MINUTES,
            bedtimeEndMinutes = parseMinutes(bedtimeEnd)
                ?.coerceIn(BEDTIME_END_RANGE_MINUTES)
                ?: DEFAULT_BEDTIME_END_MINUTES
        )

        /** Minutes after local midnight, or null when [value] is not a HH:mm time. */
        fun parseMinutes(value: String): Int? = try {
            val time = LocalTime.parse(value, STORED_FORMAT)
            time.hour * 60 + time.minute
        } catch (exception: DateTimeParseException) {
            null
        }

        private fun textOf(minutes: Int): String = "%02d:%02d".format(Locale.ROOT, minutes / 60, minutes % 60)
    }
}
