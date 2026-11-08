// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.world

import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldMotionTest {
    @Test fun stageOpeningAndRecoveryHaveAnIntermediateFrameWithoutChangingPlants() {
        val old = WorldPlant(Plant("plant", "flower", 25, 20, PlantCondition.WILTING))
        val next = WorldPlant(old.plant.copy(growthPoints = 45, health = 90, condition = PlantCondition.GROWING))
        val transition = WorldVisualTransition(listOf(old), listOf(next))
        val middle = transition.sample(450, true).single()
        assertTrue(middle.growth!! in 0.79f..0.99f)
        assertEquals(0.5f, middle.bloom!!)
        assertTrue(middle.wilt!! > 0f && middle.wilt!! < old.motion().wilt)
        assertEquals(next.plant, middle.plant)
        assertEquals(25, old.plant.growthPoints)
        assertEquals(next.motion(), transition.sample(0, false).single().motion())
    }
}
