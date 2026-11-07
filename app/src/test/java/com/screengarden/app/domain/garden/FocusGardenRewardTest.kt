// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

import com.screengarden.app.domain.focus.FocusStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusGardenRewardTest {
    private val wilting = Plant("same", "fern", 44, 10, PlantCondition.WILTING, 0)

    @Test fun completionImmediatelyRevivesTheSamePlantAndCanBloomIt() {
        val result = GardenEngine.applyFocus(FocusStatus.COMPLETED, listOf(wilting), 100)
        assertEquals("same", result.plants.single().id)
        assertEquals(46, result.plants.single().growthPoints)
        assertEquals(14, result.plants.single().health)
        assertEquals(PlantCondition.RECOVERING, result.plants.single().condition)
        assertEquals(100L, result.plants.single().lastFocusAtUtcMillis)
        assertEquals(listOf("same"), result.recovered)
        assertEquals(listOf("same"), result.bloomed)
    }

    @Test fun anyNumberOfSoftFailsRetainsThePlantStageAndHealth() {
        var plant = wilting
        repeat(1000) { plant = GardenEngine.applyFocus(FocusStatus.FAILED_SOFT, listOf(plant), 100).plants.single() }
        assertEquals(wilting.id, plant.id)
        assertEquals(PlantStage.BUD, plant.stage)
        assertEquals(25, plant.growthPoints)
        assertEquals(10, plant.health)
        assertEquals(PlantCondition.WILTING, plant.condition)
        plant = GardenEngine.applyFocus(FocusStatus.COMPLETED, listOf(plant), 100).plants.single()
        assertEquals(PlantCondition.RECOVERING, plant.condition)
    }

    @Test fun completionPlantsIntoAnEmptyGardenButCancellationDoesNot() {
        val result = GardenEngine.applyFocus(FocusStatus.COMPLETED, emptyList(), 100, newPlantId = { "seed" })
        assertEquals("seed", result.planted?.id)
        assertEquals(2, result.planted?.growthPoints)
        assertEquals(100L, result.planted?.plantedAtUtcMillis)
        assertTrue(GardenEngine.applyFocus(FocusStatus.CANCELLED, emptyList(), 100).plants.isEmpty())
    }

    @Test fun dailyAdvanceDoesNotDuplicateHealingOrExceedTheTenPointCap() {
        val initial = wilting.copy(growthPoints = 0)
        val first = GardenEngine.applyFocus(FocusStatus.COMPLETED, listOf(initial), 1).plants
        val second = GardenEngine.applyFocus(FocusStatus.COMPLETED, first, 2, completedEarlierToday = 1).plants
        val third = GardenEngine.applyFocus(FocusStatus.COMPLETED, second, 3, 2).plants.single()
        assertEquals(second.single().growthPoints, third.growthPoints)
        assertEquals(second.single().health, third.health)
        assertEquals(3L, third.lastFocusAtUtcMillis)
        val day = GardenDay(100, 2, 30, focusRewardsAlreadyApplied = true)
        val final = GardenEngine.advance(day, second, rules = GardenRules(maxPlants = 1)).plants.single()
        assertEquals(10, final.growthPoints)
        assertEquals(28, final.health) // two heals of four, then ten for the good day
        val poor = GardenEngine.grow(second.single(), day.copy(score = 0))
        assertEquals(second.single().health, poor.health) // focus still forgives the poor-day loss
    }
}
