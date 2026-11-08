// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.world

import com.screengarden.app.domain.focus.FocusSessionState
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusWorldTest {
    @Test fun previewGrowsTheLinkedPlantAndNeverMutatesStoredGrowth() {
        val seed = Plant("seed", "fern", 0)
        val world = WorldState(mapWorldPlants(listOf(seed)), space = WorldSpace.FIELD)
        val session = FocusSessionState("session", 0, 0, 25, actualDurationMs = 750_000, plantId = "seed")
        val preview = world.focusPreview(session)
        assertEquals(PlantStage.SPROUT, preview.plants.single().plant.stage)
        assertEquals(0, world.plants.single().plant.growthPoints)
        assertEquals(seed.id, preview.plants.single().plant.id)
        assertTrue(preview.plants.single().growth!! > 0f)
    }

    @Test fun anEmptyWorldCanPreviewASeedWithoutWritingAPlant() {
        val world = WorldState()
        val preview = world.focusPreview(FocusSessionState("session", 0, 0, 25))
        assertEquals(0, world.plants.size)
        assertEquals(1, preview.plants.size)
    }
}
