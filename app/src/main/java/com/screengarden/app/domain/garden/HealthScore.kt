// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

/**
 * The five things one day is judged on (AGENT_MEMORY 16.1).
 *
 * Naming each part is what makes the score explainable: the UI can say "your
 * screen time cost you 20 points" instead of showing an unexplained number.
 */
enum class HealthInput {

    /** Total screen time against the daily goal. */
    SCREEN_TIME,

    /** How far the enabled per-app limits were exceeded. */
    APP_LIMITS,

    /** Pickups against the pickup goal. */
    PICKUPS,

    /** Usage inside the bedtime window. */
    BEDTIME,

    /** Focus minutes against the focus target. */
    FOCUS
}

/**
 * One local day of raw numbers, in minutes and counts.
 *
 * A `null` field means "we do not know", which is NOT the same as zero: the
 * usage layer reports [com.screengarden.app.data.usage.DayUsageResult.MissingPermission]
 * when access is absent, and a day from before the app was installed has no
 * rows at all. Treating either as zero would hand out a perfect score for a day
 * that was never observed, so unknown parts are left out of the calculation
 * instead (see [HealthScoreCalculator]).
 *
 * A goal of zero or less is treated the same way: without a goal there is
 * nothing to compare against, and dividing by it would be meaningless.
 */
data class HealthScoreInputs(
    val screenMinutes: Int? = null,
    val dailyGoalMinutes: Int = 0,
    val pickups: Int? = null,
    val pickupGoal: Int = 0,
    val bedtimeMinutes: Int? = null,
    val focusMinutes: Int? = null,
    /**
     * One entry per enabled per-app limit, in minutes over that limit, so an
     * unbreached limit is a plain zero. An empty list or `null` means "no limits
     * are configured", which is not judged at all rather than scored as perfect
     * behaviour (AGENT_MEMORY 16.1).
     */
    val limitOveragesMinutes: List<Int>? = null
)

/**
 * How one part of the day went, kept so the number can be explained.
 *
 * [weight] is the configured weight of this part out of [HealthScore.MAX_VALUE],
 * [quality] is the 0..1 result of its curve, and [points] is what it actually
 * contributed after the remaining parts were renormalised around missing data.
 * The [points] of all components add up to (within rounding of) [HealthScore.value].
 */
data class HealthScoreComponent(val input: HealthInput, val weight: Double, val quality: Double, val points: Double)

/**
 * The score for one day: an integer from 0 to [MAX_VALUE] plus its breakdown.
 *
 * Nothing here is time-sensitive, platform-specific or persisted yet: the
 * calculator is a pure function so P08 can pin it with tests before any screen
 * or database column depends on the curve (K24 gates storing it).
 */
data class HealthScore(val value: Int, val components: List<HealthScoreComponent>) {

    /** The component for [input], or null when that part of the day was unknown. */
    fun component(input: HealthInput): HealthScoreComponent? = components.firstOrNull { it.input == input }

    companion object {

        /** A perfect day. The floor is always zero. */
        const val MAX_VALUE: Int = 100
    }
}
