// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for the plant lifecycle (AGENT_MEMORY 16.2).
 *
 * The important property is the one the product promises: a plant is never lost.
 * Everything else here (gradual wilting, recovery, the cap, one seed a day) hangs
 * off that or off the shape of the growth curve.
 *
 * Note that a good day also plants a seed, so these tests read the plant they care
 * about by id instead of assuming it is the only one in the garden.
 */
class GardenEngineTest {

    private val rules = GardenRules()

    private fun seed(
        id: String = "plant-1",
        growthPoints: Int = 0,
        health: Int = PlantTuning.MAX_HEALTH,
        condition: PlantCondition = PlantCondition.GROWING
    ) = Plant(
        id = id,
        species = "fern",
        growthPoints = growthPoints,
        health = health,
        condition = condition,
        plantedAtUtcMillis = 1_000L
    )

    private fun day(score: Int?, focus: Int = 0, streak: Int = 0) = GardenDay(
        score = score,
        focusSessionsCompleted = focus,
        streakDays = streak,
        dayStartUtcMillis = DAY_MILLIS,
        lastFocusAtUtcMillis = if (focus > 0) FOCUS_MILLIS else null
    )

    /** Applies the same day [times] times and returns the resulting plants. */
    private fun runDays(start: Plant, times: Int, score: Int?, focus: Int = 0, streak: Int = 0): List<Plant> {
        var plants = listOf(start)
        repeat(times) { plants = GardenEngine.advance(day(score, focus, streak), plants, rules).plants }
        return plants
    }

    /** The same plant after [days] of the same day. */
    private fun grown(start: Plant = seed(), days: Int = 1, score: Int?, focus: Int = 0, streak: Int = 0): Plant =
        runDays(start, days, score, focus, streak).first { it.id == start.id }

    @Test
    fun theStageFollowsTheGrowthPoints() {
        assertEquals(PlantStage.SEED, PlantStage.forGrowthPoints(0))
        assertEquals(PlantStage.SEED, PlantStage.forGrowthPoints(PlantTuning.SPROUT_AT - 1))
        assertEquals(PlantStage.SPROUT, PlantStage.forGrowthPoints(PlantTuning.SPROUT_AT))
        assertEquals(PlantStage.BUD, PlantStage.forGrowthPoints(PlantTuning.BUD_AT))
        assertEquals(PlantStage.BUD, PlantStage.forGrowthPoints(PlantTuning.BLOOM_AT - 1))
        assertEquals(PlantStage.BLOOM, PlantStage.forGrowthPoints(PlantTuning.BLOOM_AT))
        assertEquals(PlantStage.BLOOM, PlantStage.forGrowthPoints(1_000))
    }

    @Test
    fun negativeGrowthPointsStillCountAsASeed() {
        assertEquals(PlantStage.SEED, PlantStage.forGrowthPoints(-50))
        assertEquals(PlantStage.SEED, seed(growthPoints = -50).stage)
    }

    @Test
    fun aPerfectDayAddsTheFullScoreGrowth() {
        assertEquals(GardenTuning.GROWTH_PER_FULL_SCORE, grown(score = 100).growthPoints)
    }

    @Test
    fun aScoreBelowSixtyStillGrowsButLess() {
        val perfect = grown(score = 100)
        val average = grown(score = 40)

        assertTrue(average.growthPoints < perfect.growthPoints)
        assertEquals(2, average.growthPoints)
    }

    @Test
    fun focusSessionsAddGrowthUpToTheCap() {
        val one = grown(score = 0, focus = 1)
        val many = grown(score = 0, focus = 9)

        assertEquals(GardenTuning.GROWTH_PER_FOCUS, one.growthPoints)
        assertEquals(GardenTuning.GROWTH_PER_FOCUS * GardenTuning.MAX_COUNTED_FOCUS, many.growthPoints)
    }

    @Test
    fun theStreakBonusStartsOnTheThirdDay() {
        assertEquals(0, grown(score = 0, streak = 2).growthPoints)
        assertEquals(1, grown(score = 0, streak = 3).growthPoints)
        assertEquals(GardenTuning.MAX_STREAK_BONUS, grown(score = 0, streak = 30).growthPoints)
    }

    @Test
    fun growthIsCappedPerDay() {
        assertEquals(
            GardenTuning.MAX_GROWTH_PER_DAY,
            grown(score = 100, focus = 9, streak = 30).growthPoints
        )
    }

    @Test
    fun anUnknownScoreDoesNotGrowOrPunish() {
        val out = grown(seed(growthPoints = 20, health = 80), score = null)

        assertEquals(20, out.growthPoints)
        assertEquals(80, out.health)
        assertEquals(PlantCondition.GROWING, out.condition)
    }

    @Test
    fun focusStillHealsOnAnUnknownDay() {
        val out = grown(seed(health = 40), score = null, focus = 1)

        assertEquals(40 + GardenTuning.HEALTH_PER_FOCUS, out.health)
        assertEquals(PlantCondition.RECOVERING, out.condition)
    }

    @Test
    fun aGoodDayHealsAWiltedPlant() {
        val wilted = seed(health = 20, condition = PlantCondition.WILTING)
        val out = GardenEngine.advance(day(score = 100), listOf(wilted), rules)

        val healed = requireNotNull(out.plant("plant-1"))
        assertEquals(30, healed.health)
        assertTrue(healed.isRecovering)
        assertEquals(listOf("plant-1"), out.recovered)
    }

    @Test
    fun aPoorDayWiltsGradually() {
        val out = grown(seed(health = 100), score = 10)

        assertEquals(100 - GardenTuning.WILT_PER_POOR_DAY, out.health)
        assertEquals("One bad day must not wilt a full plant", PlantCondition.GROWING, out.condition)
    }

    @Test
    fun wiltingTakesSeveralDaysRatherThanOne() {
        assertFalse(grown(days = 6, score = 10).isWilting)
        assertTrue("Seven poor days should show on the plant", grown(days = 7, score = 10).isWilting)
    }

    @Test
    fun healthNeverDropsBelowTheFloor() {
        assertEquals(PlantTuning.MIN_HEALTH, grown(days = 400, score = 0).health)
    }

    @Test
    fun aPlantIsNeverPermanentlyLost() {
        val starved = grown(days = 400, score = 0)
        val firstGoodDay = grown(start = starved, score = 100)
        val healed = grown(start = firstGoodDay, days = 9, score = 100)

        assertNotNull("A plant must survive any number of poor days", starved.id)
        assertEquals("plant-1", starved.id)
        assertEquals(PlantTuning.MIN_HEALTH, starved.health)
        assertTrue("One good day must move the plant back", firstGoodDay.health > starved.health)
        assertEquals(PlantCondition.GROWING, healed.condition)
        assertEquals(PlantTuning.MAX_HEALTH, healed.health)
        assertEquals(PlantStage.BLOOM, healed.stage)
    }

    @Test
    fun wiltingNeverTakesGrowthPointsAway() {
        val after = grown(seed(growthPoints = 30, health = 100), days = 20, score = 0)

        assertEquals(30, after.growthPoints)
        assertEquals(PlantStage.BUD, after.stage)
    }

    @Test
    fun aGoodDayPlantsOneSeed() {
        val out = GardenEngine.advance(day(score = 100), emptyList(), rules, newPlantId = { "new-seed" })

        val planted = out.planted
        assertNotNull(planted)
        assertEquals(1, out.plants.size)
        assertEquals("new-seed", planted?.id)
        assertEquals(PlantStage.SEED, planted?.stage)
        assertEquals(PlantTuning.NEW_PLANT_HEALTH, planted?.health)
        assertEquals(PlantCondition.GROWING, planted?.condition)
        assertEquals(DAY_MILLIS, planted?.plantedAtUtcMillis)
    }

    @Test
    fun anAverageDayPlantsNothing() {
        assertNull(GardenEngine.advance(day(score = 59), emptyList(), rules).planted)
    }

    @Test
    fun anUnknownDayPlantsNothing() {
        assertNull(GardenEngine.advance(day(score = null), emptyList(), rules).planted)
    }

    @Test
    fun plantingStopsAtTheCap() {
        val full = (1..GardenTuning.MAX_PLANTS).map { seed(id = "plant-$it") }
        val out = GardenEngine.advance(day(score = 100), full, rules, newPlantId = { "extra" })

        assertNull(out.planted)
        assertEquals(GardenTuning.MAX_PLANTS, out.plants.size)
    }

    @Test
    fun onlyOneSeedIsPlantedPerDay() {
        val firstDay = GardenEngine.advance(day(score = 100), emptyList(), rules, newPlantId = { "seed-1" })
        val secondDay = GardenEngine.advance(day(score = 100), firstDay.plants, rules, newPlantId = { "seed-2" })

        assertEquals(1, firstDay.plants.size)
        assertEquals(2, secondDay.plants.size)
        assertEquals("seed-2", secondDay.planted?.id)
    }

    @Test
    fun reachingBloomIsReportedOnce() {
        val almost = seed(growthPoints = PlantTuning.BLOOM_AT - 1)
        val bloomDay = GardenEngine.advance(day(score = 100), listOf(almost), rules)
        val nextDay = GardenEngine.advance(day(score = 100), bloomDay.plants, rules)

        assertEquals(listOf("plant-1"), bloomDay.bloomed)
        assertEquals(PlantStage.BLOOM, bloomDay.plant("plant-1")?.stage)
        assertTrue("A bloom is not news twice", nextDay.bloomed.isEmpty())
    }

    @Test
    fun wiltingAndRecoveryAreReported() {
        var plants = listOf(seed())
        repeat(6) { plants = GardenEngine.advance(day(score = 10), plants, rules).plants }

        val wiltDay = GardenEngine.advance(day(score = 10), plants, rules)
        val recoveryDay = GardenEngine.advance(day(score = 100), wiltDay.plants, rules)

        assertEquals(listOf("plant-1"), wiltDay.wilted)
        assertEquals(listOf("plant-1"), recoveryDay.recovered)
        assertTrue("A recovering plant is not a newly wilting one", recoveryDay.wilted.isEmpty())
    }

    @Test
    fun plantsKeepTheirOrderAndAreAllUpdated() {
        val plants = listOf(seed(id = "a", growthPoints = 1), seed(id = "b", growthPoints = 2))
        val out = GardenEngine.advance(day(score = 80), plants, rules)

        assertEquals(listOf("a", "b"), out.plants.take(2).map { it.id })
        assertTrue(out.plants.take(2).all { it.growthPoints > 0 })
        assertEquals("A good day also seeds one new plant", 3, out.plants.size)
    }

    @Test
    fun aFocusSessionStampsThePlant() {
        val out = GardenEngine.advance(day(score = 90, focus = 1), listOf(seed()), rules)

        assertEquals(FOCUS_MILLIS, out.plant("plant-1")?.lastFocusAtUtcMillis)
    }

    @Test
    fun aSingleFocusCompletionStartsRecoveryEvenOnAPoorDay() {
        val plant = seed(health = PlantTuning.MIN_HEALTH, condition = PlantCondition.WILTING)
        val out = GardenEngine.advance(day(score = 0, focus = 1), listOf(plant), rules)

        assertEquals(PlantTuning.MIN_HEALTH + GardenTuning.HEALTH_PER_FOCUS, out.plant(plant.id)?.health)
        assertEquals(PlantCondition.RECOVERING, out.plant(plant.id)?.condition)
        assertEquals(listOf(plant.id), out.recovered)
    }

    @Test
    fun growthNeverWrapsABloomBackToASeed() {
        val out = grown(seed(growthPoints = Int.MAX_VALUE), score = 100)

        assertEquals(Int.MAX_VALUE, out.growthPoints)
        assertEquals(PlantStage.BLOOM, out.stage)
    }

    @Test
    fun hugeConfigurableRewardsStillRespectTheGrowthAndHealthBounds() {
        val hugeRules = GardenRules(
            growthPerFullScore = Int.MAX_VALUE,
            growthPerFocus = Int.MAX_VALUE,
            maxCountedFocus = Int.MAX_VALUE,
            healthGainPerGoodDay = Int.MAX_VALUE,
            healthPerFocus = Int.MAX_VALUE
        )
        val out = GardenEngine.grow(seed(health = 50), day(score = 100, focus = Int.MAX_VALUE), hugeRules)

        assertEquals(GardenTuning.MAX_GROWTH_PER_DAY, out.growthPoints)
        assertEquals(PlantTuning.MAX_HEALTH, out.health)
    }

    @Test
    fun anUnknownDayDoesNotRewardAnOldStreakOrChangeRecovery() {
        val plant = seed(growthPoints = 20, health = 40, condition = PlantCondition.RECOVERING)

        assertEquals(plant, GardenEngine.grow(plant, day(score = null, streak = 30)))
    }

    @Test
    fun negativeCountsDoNotRewardGrowthOrFocus() {
        val out = grown(seed(), score = -10, focus = -5, streak = -5)

        assertEquals(0, out.growthPoints)
        assertEquals(PlantTuning.MAX_HEALTH - GardenTuning.WILT_PER_POOR_DAY, out.health)
        assertNull(out.lastFocusAtUtcMillis)
    }

    @Test
    fun anOversizedExistingGardenIsPreservedWithoutPlantingMore() {
        val plants = (1..GardenTuning.MAX_PLANTS + 1).map { seed(id = "plant-$it") }
        val out = GardenEngine.advance(day(score = 100), plants, rules)

        assertEquals(plants.map { it.id }, out.plants.map { it.id })
        assertNull(out.planted)
    }

    @Test
    fun aMissingFocusTimestampDoesNotInventMidnightAsTheCompletionTime() {
        val plant = seed().copy(lastFocusAtUtcMillis = FOCUS_MILLIS - 1)
        val out = GardenEngine.grow(plant, GardenDay(score = null, focusSessionsCompleted = 1))

        assertEquals(plant.lastFocusAtUtcMillis, out.lastFocusAtUtcMillis)
    }

    @Test
    fun anOlderCompletionDoesNotMoveTheFocusTimestampBackwards() {
        val plant = seed().copy(lastFocusAtUtcMillis = FOCUS_MILLIS + 1)

        assertEquals(
            plant.lastFocusAtUtcMillis,
            GardenEngine.grow(plant, day(score = 100, focus = 1)).lastFocusAtUtcMillis
        )
    }

    @Test
    fun aNewSeedDoesNotClaimSessionsCompletedBeforeItWasPlanted() {
        val out = GardenEngine.advance(day(score = 100, focus = 1), emptyList())

        assertNotNull(out.planted)
        assertNull(out.planted?.lastFocusAtUtcMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun theIdFactoryCannotPlantADuplicateId() {
        GardenEngine.advance(day(score = 100), listOf(seed()), newPlantId = { "plant-1" })
    }

    @Test
    fun aCustomCapIsHonouredWithoutCallingTheIdFactory() {
        val out = GardenEngine.advance(day(score = 100), listOf(seed()), GardenRules(maxPlants = 1)) {
            error("A full garden must not request an id")
        }

        assertEquals(1, out.plants.size)
        assertNull(out.planted)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aGardenCapOfZeroIsRejected() {
        GardenRules(maxPlants = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aPoorDayAboveTheGoodDayIsRejected() {
        GardenRules(goodDayScore = 60, poorDayScore = 80)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aHealthyLineOutsideTheBoundsIsRejected() {
        GardenRules(minHealth = 50, maxHealth = 100, healthyAt = 20)
    }

    private companion object {
        const val DAY_MILLIS = 1_700_000_000_000L
        const val FOCUS_MILLIS = DAY_MILLIS + 3_600_000L
    }
}
