// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.world.RenderMode
import com.screengarden.app.domain.world.WorldState
import com.screengarden.app.domain.world.mapWorldPlants
import com.screengarden.app.ui.world.drawWorld
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WorldRendererTest {
    @Test fun fieldPlantsStayRootedBelowTheMeadowHorizon() {
        val state = WorldState(
            plants = mapWorldPlants((0..30).map { Plant("field-$it", "fern", 45, plantedAtUtcMillis = it.toLong()) }),
            space = com.screengarden.app.domain.world.WorldSpace.FIELD
        )
        val visible = com.screengarden.app.ui.world.visibleWorldPlants(state, RenderMode.INTERACTIVE, 600f, 400f)
        assertTrue(visible.isNotEmpty())
        assertTrue(
            visible.all {
                val rect = com.screengarden.app.ui.world.worldPlantRect(it, state, 600f, 400f)
                rect[1] + rect[3] >= 400f * 0.49f
            }
        )
    }

    @Test fun explicitIndividualPlantSwaySurvivesTheSharedRenderer() {
        var sway = 0f
        val image = ImageBitmap(100, 100)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(100f, 100f)) {
            drawWorld(
                WorldState(
                    plants = listOf(
                        com.screengarden.app.domain.world.WorldPlant(Plant("sway", "fern", 45), swayDegrees = 1.2f)
                    ),
                    backdrop = false
                ),
                RenderMode.INTERACTIVE,
                lightColorScheme(),
                PlantRenderer { _, _, _, visuals -> sway = visuals.swayDegrees }
            )
        }
        org.junit.Assert.assertEquals(1.2f, sway)
    }

    @Test fun viewportCullingKeepsDistantPlantsAvailableBeyondTheDrawCap() {
        val plants = com.screengarden.app.domain.world.mapWorldPlants(
            (0..499).map {
                Plant("plant-$it", "fern", 45, plantedAtUtcMillis = it.toLong())
            }
        )
        val target = plants[400]
        val state = WorldState(
            plants,
            camera = com.screengarden.app.domain.world.WorldCamera(target.x, target.y),
            space = com.screengarden.app.domain.world.WorldSpace.FIELD
        )
        val visible = com.screengarden.app.ui.world.visibleWorldPlants(state, RenderMode.INTERACTIVE, 600f, 400f)
        assertTrue(visible.any { it.plant.id == target.plant.id })
        assertTrue(visible.size <= RenderMode.INTERACTIVE.policy.maxPlants)
    }

    @Test fun snapshotAdapterMatchesTheSharedRendererPixels() {
        val colors = lightColorScheme()
        val plants = listOf(Plant("old-fern", "fern", 15), Plant("old-flower", "flower", 45))
        val old = renderGardenSnapshot(GardenUiState(plants = plants), colors, framed = false).asAndroidBitmap()
        val image = ImageBitmap(1024, 768)
        CanvasDrawScope().draw(Density(2f), LayoutDirection.Ltr, Canvas(image), Size(1024f, 768f)) {
            drawWorld(
                WorldState(plants = mapWorldPlants(plants), space = com.screengarden.app.domain.world.WorldSpace.FIELD),
                RenderMode.WALLPAPER,
                colors,
                VectorPlantRenderer
            )
        }
        assertTrue("Snapshot adapter must use the same world geometry", old.sameAs(image.asAndroidBitmap()))
    }
}
