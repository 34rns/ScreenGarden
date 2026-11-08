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
import com.screengarden.app.domain.world.mapPlotPlants
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
    @Test fun snapshotAdapterMatchesTheSharedRendererPixels() {
        val colors = lightColorScheme()
        val plants = listOf(Plant("old-fern", "fern", 15), Plant("old-flower", "flower", 45))
        val old = renderGardenSnapshot(GardenUiState(plants = plants), colors, framed = false).asAndroidBitmap()
        val image = ImageBitmap(1024, 768)
        CanvasDrawScope().draw(Density(2f), LayoutDirection.Ltr, Canvas(image), Size(1024f, 768f)) {
            drawWorld(WorldState(plants = mapPlotPlants(plants)), RenderMode.SNAPSHOT, colors, VectorPlantRenderer)
        }
        assertTrue("Snapshot adapter must use the same world geometry", old.sameAs(image.asAndroidBitmap()))
    }
}
