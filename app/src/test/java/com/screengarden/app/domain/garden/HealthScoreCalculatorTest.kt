// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for the daily health score (AGENT_MEMORY 16.1).
 *
 * The point of these tests is the shape of the curves, not one magic total: the
 * score must stay explainable, smooth and honest about missing data, and those
 * are exactly the properties a later refactor could quietly break.
 */
class HealthScoreCalculatorTest {

    private fun perfectDay() = HealthScoreInputs(
        screenMinutes = 90,
        dailyGoalMinutes = 180,
        pickups = 40,
        pickupGoal = 80,
        bedtimeMinutes = 0,
        focusMinutes = HealthScoreTuning.FOCUS_TARGET_MINUTES,
        limitOveragesMinutes = listOf(0, 0)
    )

    private fun scoreOf(inputs: HealthScoreInputs): HealthScore {
        val score = HealthScoreCalculator.calculate(inputs)
        assertNotNull("Expected a score for $inputs", score)
        return requireNotNull(score)
    }

    @Test
    fun aDayInsideEveryGoalScoresOneHundred() {
        assertEquals(HealthScore.MAX_VALUE, scoreOf(perfectDay()).value)
    }

    @Test
    fun zeroUsageWithoutFocusOnlyLosesTheFocusWeight() {
        // Nothing was used, so every "less is better" part is full marks; only the
        // achievement part (focus) has nothing to show.
        val score = scoreOf(
            perfectDay().copy(screenMinutes = 0, pickups = 0, bedtimeMinutes = 0, focusMinutes = 0)
        )

        assertEquals(85, score.value)
        assertEquals(0.0, requireNotNull(score.component(HealthInput.FOCUS)).quality, 0.0001)
    }

    @Test
    fun missingDataIsLeftOutInsteadOfCountedAsZero() {
        val withEverything = scoreOf(perfectDay())
        val withoutScreenTime = scoreOf(perfectDay().copy(screenMinutes = null))

        assertEquals(HealthScore.MAX_VALUE, withEverything.value)
        assertEquals(HealthScore.MAX_VALUE, withoutScreenTime.value)
        assertNull(withoutScreenTime.component(HealthInput.SCREEN_TIME))
        assertEquals(4, withoutScreenTime.components.size)
    }

    @Test
    fun noDataAtAllHasNoScore() {
        assertNull(HealthScoreCalculator.calculate(HealthScoreInputs()))
        assertNull(
            HealthScoreCalculator.calculate(
                HealthScoreInputs(screenMinutes = 120, dailyGoalMinutes = 0, pickups = 10, pickupGoal = 0)
            )
        )
    }

    @Test
    fun aGoalSizedOvershootKeepsAboutAThirdOfTheScreenPart() {
        val score = scoreOf(perfectDay().copy(screenMinutes = 2 * 180))

        val screen = requireNotNull(score.component(HealthInput.SCREEN_TIME))
        assertEquals(expOf(-HealthScoreTuning.OVERSHOOT_DECAY), screen.quality, 0.0001)
        assertTrue("Two hours over must cost real points", score.value < 90)
        assertTrue("Two hours over must not wipe the day", score.value > 55)
    }

    @Test
    fun screenTimeIsScoredSmoothlyWithNoCliff() {
        var previous: Int? = null
        for (minutes in 0..1_200) {
            val value = scoreOf(perfectDay().copy(screenMinutes = minutes)).value
            previous?.let { last ->
                assertTrue("Score jumped at ${minutes}m: $last -> $value", abs(value - last) <= 1)
            }
            previous = value
        }
    }

    @Test
    fun oneWildInputCannotZeroTheDay() {
        val score = scoreOf(perfectDay().copy(screenMinutes = 50 * 180))

        assertTrue("A broken number should not reach zero: ${score.value}", score.value >= 60)
        // A thousand pickups cost their own 20 points and nothing else.
        assertEquals(80, scoreOf(perfectDay().copy(pickups = 1_000, pickupGoal = 80)).value)
    }

    @Test
    fun onGoalPickupsKeepTheWholePickupWeight() {
        val score = scoreOf(perfectDay().copy(pickups = 80))

        assertEquals(1.0, requireNotNull(score.component(HealthInput.PICKUPS)).quality, 0.0001)
        assertEquals(HealthScore.MAX_VALUE, score.value)
    }

    @Test
    fun doubleThePickupGoalCostsPickupPointsSmoothly() {
        val score = scoreOf(perfectDay().copy(pickups = 160))

        val pickups = requireNotNull(score.component(HealthInput.PICKUPS))
        assertEquals(expOf(-HealthScoreTuning.OVERSHOOT_DECAY), pickups.quality, 0.0001)
        assertEquals(87, score.value)
    }

    @Test
    fun bedtimeGraceMinutesAreFree() {
        val score = scoreOf(perfectDay().copy(bedtimeMinutes = HealthScoreTuning.BEDTIME_GRACE_MINUTES))

        assertEquals(1.0, requireNotNull(score.component(HealthInput.BEDTIME)).quality, 0.0001)
        assertEquals(HealthScore.MAX_VALUE, score.value)
    }

    @Test
    fun bedtimeBeyondTheGraceDecays() {
        val grace = HealthScoreTuning.BEDTIME_GRACE_MINUTES
        val oneUnitLate = scoreOf(perfectDay().copy(bedtimeMinutes = 2 * grace))
        val farTooLate = scoreOf(perfectDay().copy(bedtimeMinutes = 20 * grace))

        assertEquals(
            expOf(-HealthScoreTuning.OVERSHOOT_DECAY),
            requireNotNull(oneUnitLate.component(HealthInput.BEDTIME)).quality,
            0.0001
        )
        assertTrue(farTooLate.value < oneUnitLate.value)
    }

    @Test
    fun focusRampsLinearlyToItsTarget() {
        val target = HealthScoreTuning.FOCUS_TARGET_MINUTES
        val half = requireNotNull(scoreOf(perfectDay().copy(focusMinutes = target / 2)).component(HealthInput.FOCUS))
        val full = requireNotNull(scoreOf(perfectDay().copy(focusMinutes = target)).component(HealthInput.FOCUS))
        val beyondTarget =
            requireNotNull(scoreOf(perfectDay().copy(focusMinutes = target * 3)).component(HealthInput.FOCUS))

        assertEquals(0.5, half.quality, 0.0001)
        assertEquals(1.0, full.quality, 0.0001)
        assertEquals("Focus must not earn more than its weight", 1.0, beyondTarget.quality, 0.0001)
    }

    @Test
    fun unconfiguredLimitsAreNotJudged() {
        val score = scoreOf(perfectDay().copy(limitOveragesMinutes = emptyList()))

        assertNull(score.component(HealthInput.APP_LIMITS))
        assertEquals(HealthScore.MAX_VALUE, score.value)
    }

    @Test
    fun anUnbreachedConfiguredLimitKeepsTheWholeLimitWeight() {
        val score = scoreOf(perfectDay().copy(limitOveragesMinutes = listOf(0, 0, 0)))

        assertEquals(1.0, requireNotNull(score.component(HealthInput.APP_LIMITS)).quality, 0.0001)
        assertEquals(HealthScore.MAX_VALUE, score.value)
    }

    @Test
    fun aBreachedLimitCostsPartOfTheLimitWeight() {
        val clean = scoreOf(perfectDay().copy(limitOveragesMinutes = listOf(0)))
        val breached = scoreOf(perfectDay().copy(limitOveragesMinutes = listOf(0, 120)))

        val limits = requireNotNull(breached.component(HealthInput.APP_LIMITS))
        assertEquals(expOf(-2 * HealthScoreTuning.OVERSHOOT_DECAY), limits.quality, 0.0001)
        assertTrue("A breach must cost points", breached.value < clean.value)
        assertTrue("An optional feature must not wipe the day", breached.value > 80)
    }

    @Test
    fun aGoalOfZeroIsNotJudged() {
        val score = scoreOf(perfectDay().copy(dailyGoalMinutes = 0, pickupGoal = 0))

        assertNull(score.component(HealthInput.SCREEN_TIME))
        assertNull(score.component(HealthInput.PICKUPS))
        assertEquals(HealthScore.MAX_VALUE, score.value)
    }

    @Test
    fun componentsAddUpToTheShownValue() {
        val score = scoreOf(
            HealthScoreInputs(
                screenMinutes = 400,
                dailyGoalMinutes = 180,
                pickups = 130,
                pickupGoal = 80,
                bedtimeMinutes = 45,
                focusMinutes = 20,
                limitOveragesMinutes = listOf(0, 25)
            )
        )

        // The shown value is the rounded sum, so the parts agree to within half a point.
        assertEquals(score.value.toDouble(), score.components.sumOf { it.points }, 0.5)
        assertTrue(score.value in 0..HealthScore.MAX_VALUE)
    }

    @Test
    fun absurdInputsStayInsideZeroToOneHundred() {
        val extreme = HealthScoreInputs(
            screenMinutes = Int.MAX_VALUE,
            dailyGoalMinutes = 1,
            pickups = Int.MAX_VALUE,
            pickupGoal = 1,
            bedtimeMinutes = Int.MAX_VALUE,
            focusMinutes = 0,
            limitOveragesMinutes = listOf(Int.MAX_VALUE)
        )

        assertTrue(scoreOf(extreme).value in 0..HealthScore.MAX_VALUE)
        assertEquals(0, scoreOf(extreme).value)
    }

    @Test
    fun negativeInputsAreClampedInsteadOfRewarded() {
        val score = scoreOf(perfectDay().copy(screenMinutes = -60, pickups = -10, bedtimeMinutes = -5))

        assertEquals(1.0, requireNotNull(score.component(HealthInput.SCREEN_TIME)).quality, 0.0001)
        assertEquals(HealthScore.MAX_VALUE, score.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeWeightsAreRejected() {
        HealthScoreWeights(screenTime = -1.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aZeroCurveSettingIsRejected() {
        HealthScoreWeights(overshootDecay = -1.0)
    }

    @Test
    fun shippedWeightsFillExactlyOneHundredPoints() {
        assertEquals(100.0, HealthScoreWeights().totalWeight, 0.0001)
    }

    private fun expOf(exponent: Double): Double = kotlin.math.exp(exponent)
}
