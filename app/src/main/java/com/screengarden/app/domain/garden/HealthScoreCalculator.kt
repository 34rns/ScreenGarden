// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * The numbers behind the score, in one place so they can be tuned without
 * reading the arithmetic (AGENT_MEMORY 16.1).
 *
 * The five weights add up to 100, i.e. to a full [HealthScore.MAX_VALUE], so
 * "screen time is worth 35 of the 100 points" is literally true.
 */
object HealthScoreTuning {

    /** Screen time against the daily goal: the largest single part. */
    const val SCREEN_TIME_WEIGHT: Double = 35.0

    /** Per-app limit breaches. The smallest part: limits are optional. */
    const val APP_LIMITS_WEIGHT: Double = 15.0

    /** Pickups against the pickup goal. */
    const val PICKUPS_WEIGHT: Double = 20.0

    /** Usage inside the bedtime window. */
    const val BEDTIME_WEIGHT: Double = 15.0

    /** Focus minutes against the focus target. */
    const val FOCUS_WEIGHT: Double = 15.0

    /**
     * How fast an overshoot eats a part. Each goal-sized overshoot multiplies
     * what is left of that part by `e^-1` (about 0.37), so twice the goal still
     * keeps roughly a third and nothing ever snaps to zero.
     */
    const val OVERSHOOT_DECAY: Double = 1.0

    /** Every this many minutes over an app limit multiplies the limit part by `e^-1`. */
    const val LIMIT_OVERAGE_UNIT_MINUTES: Int = 60

    /** Minutes inside the bedtime window that are free before the bedtime part decays. */
    const val BEDTIME_GRACE_MINUTES: Int = 15

    /**
     * Focus minutes that earn the whole focus part. There is no settings key for a
     * focus target yet (16.7 lists none and D40 forbids inventing one), so this is
     * the documented tuning constant until a prompt asks for it.
     */
    const val FOCUS_TARGET_MINUTES: Int = 60
}

/**
 * The weights and curve settings the calculator uses.
 *
 * A data class rather than loose constants so a test (or a future "gentle vs
 * strict" setting) can pass a different set without touching the arithmetic,
 * while [HealthScoreTuning] stays the single source of the shipped defaults.
 */
data class HealthScoreWeights(
    val screenTime: Double = HealthScoreTuning.SCREEN_TIME_WEIGHT,
    val appLimits: Double = HealthScoreTuning.APP_LIMITS_WEIGHT,
    val pickups: Double = HealthScoreTuning.PICKUPS_WEIGHT,
    val bedtime: Double = HealthScoreTuning.BEDTIME_WEIGHT,
    val focus: Double = HealthScoreTuning.FOCUS_WEIGHT,
    val overshootDecay: Double = HealthScoreTuning.OVERSHOOT_DECAY,
    val limitOverageUnitMinutes: Int = HealthScoreTuning.LIMIT_OVERAGE_UNIT_MINUTES,
    val bedtimeGraceMinutes: Int = HealthScoreTuning.BEDTIME_GRACE_MINUTES,
    val focusTargetMinutes: Int = HealthScoreTuning.FOCUS_TARGET_MINUTES
) {

    init {
        require(screenTime >= 0.0 && appLimits >= 0.0 && pickups >= 0.0 && bedtime >= 0.0 && focus >= 0.0) {
            "Health score weights must not be negative"
        }
        require(totalWeight > 0.0) { "At least one health score weight must be positive" }
        require(overshootDecay >= 0.0) { "overshootDecay must not be negative" }
        require(limitOverageUnitMinutes >= 1) { "limitOverageUnitMinutes must be at least 1" }
        require(bedtimeGraceMinutes >= 1) { "bedtimeGraceMinutes must be at least 1" }
        require(focusTargetMinutes >= 1) { "focusTargetMinutes must be at least 1" }
    }

    /** Sum of the five part weights; 100 with the shipped defaults. */
    val totalWeight: Double get() = screenTime + appLimits + pickups + bedtime + focus
}

/**
 * Turns one day of plain numbers into a 0-100 health score (AGENT_MEMORY 16.1).
 *
 * The formula, in plain language:
 *  - Screen time is free up to the daily goal. Every further goal-length of use
 *    multiplies what is left of its 35 points by `e^-1` (about 0.37), so one
 *    minute over the goal costs almost nothing and twice the goal still keeps
 *    roughly a third. Pickups work the same way against the pickup goal, and the
 *    bedtime window works the same way after a free grace of 15 minutes.
 *  - The per-app limits are judged by how far they were exceeded in total: every
 *    60 minutes over multiplies the limit part by the same factor.
 *  - Focus is the one part that rewards: it ramps linearly from zero to its full
 *    15 points at the focus target.
 *  - Every curve is continuous, so no single minute can move the score by more
 *    than about half a point, and one bad input cannot zero the day.
 *  - Parts with no data are left out and the remaining weights are renormalised,
 *    so an unwatched evening is not counted as perfect behaviour.
 *
 * Nothing here is persisted: `DailyStat.score` is the v2 column that needs a
 * Migration (K24), which P08 deliberately does not add.
 */
object HealthScoreCalculator {

    /**
     * Scores [inputs], or returns null when there is nothing to judge.
     *
     * Null is the honest answer for a day where no part applies (no usage data,
     * no goals, no focus): returning 0 would accuse the user of a day that was
     * never observed, and returning 100 would flatter them. The Insights empty
     * state is P19's job (K45).
     */
    fun calculate(inputs: HealthScoreInputs, weights: HealthScoreWeights = HealthScoreWeights()): HealthScore? {
        val judged = mutableListOf<Judged>()

        inputs.screenMinutes?.let { minutes ->
            addIfWeighted(judged, HealthInput.SCREEN_TIME, weights.screenTime) {
                budgetQuality(
                    usedMinutes = minutes,
                    budgetMinutes = inputs.dailyGoalMinutes,
                    decay = weights.overshootDecay
                )
            }
        }
        addIfWeighted(judged, HealthInput.APP_LIMITS, weights.appLimits) {
            limitsQuality(inputs.limitOveragesMinutes, weights)
        }
        inputs.pickups?.let { pickups ->
            addIfWeighted(judged, HealthInput.PICKUPS, weights.pickups) {
                budgetQuality(usedMinutes = pickups, budgetMinutes = inputs.pickupGoal, decay = weights.overshootDecay)
            }
        }
        inputs.bedtimeMinutes?.let { minutes ->
            addIfWeighted(judged, HealthInput.BEDTIME, weights.bedtime) {
                budgetQuality(
                    usedMinutes = minutes,
                    budgetMinutes = weights.bedtimeGraceMinutes,
                    decay = weights.overshootDecay
                )
            }
        }
        inputs.focusMinutes?.let { minutes ->
            addIfWeighted(judged, HealthInput.FOCUS, weights.focus) {
                (minutes.coerceAtLeast(0).toDouble() / weights.focusTargetMinutes).coerceAtMost(1.0)
            }
        }

        if (judged.isEmpty()) return null
        val applicableWeight = judged.sumOf { it.weight }
        val components = judged.map { part ->
            HealthScoreComponent(
                input = part.input,
                weight = part.weight,
                quality = part.quality,
                points = part.weight * part.quality / applicableWeight * HealthScore.MAX_VALUE
            )
        }
        val value = components.sumOf { it.points }.roundToInt().coerceIn(0, HealthScore.MAX_VALUE)
        return HealthScore(value = value, components = components)
    }

    /**
     * The shared "less is better" curve: full marks up to the budget, then a smooth
     * exponential decay instead of a step. Returns null when the budget is unknown.
     */
    private fun budgetQuality(usedMinutes: Int, budgetMinutes: Int, decay: Double): Double? {
        if (budgetMinutes <= 0) return null
        val ratio = usedMinutes.coerceAtLeast(0).toDouble() / budgetMinutes
        if (ratio <= 1.0) return 1.0
        return exp(-decay * (ratio - 1.0))
    }

    /**
     * Sums the overages of the enabled limits. Null or empty means no limits are
     * configured, which is not judged; an empty-but-configured set arrives as a
     * list of zeros and scores full marks.
     */
    private fun limitsQuality(overages: List<Int>?, weights: HealthScoreWeights): Double? {
        if (overages.isNullOrEmpty()) return null
        val totalOverage = overages.sumOf { it.coerceAtLeast(0) }
        if (totalOverage <= 0) return 1.0
        return exp(-weights.overshootDecay * totalOverage / weights.limitOverageUnitMinutes)
    }

    /** Records [quality] for [input] only when the part is weighted and judgeable. */
    private inline fun addIfWeighted(
        judged: MutableList<Judged>,
        input: HealthInput,
        weight: Double,
        quality: () -> Double?
    ) {
        if (weight <= 0.0) return
        val value = quality() ?: return
        judged += Judged(input = input, weight = weight, quality = value.coerceIn(0.0, 1.0))
    }
}

/** A part of the day that could be judged, before the weights are renormalised. */
private data class Judged(val input: HealthInput, val weight: Double, val quality: Double)
