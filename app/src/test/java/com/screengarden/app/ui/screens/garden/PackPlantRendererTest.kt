// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.data.plants.AssetPlantPackSource
import com.screengarden.app.data.plants.PlantPackLoader
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PackPlantRendererTest {
    private val colors = lightColorScheme()
    private val state = GardenUiState(plants = listOf(Plant("fake", "flower", 45, 80)))

    @Test fun missingPackKeepsTheVectorFallbackInExportedImages() {
        val expected = renderGardenSnapshot(state, colors).asAndroidBitmap()
        val actual = renderGardenSnapshot(state, colors, renderer = PackPlantRenderer(emptyMap())).asAndroidBitmap()
        assertTrue(expected.sameAs(actual))
    }

    @Test fun bundledStageArtIsUsedBySnapshotsAndWiltingChangesItsPixels() {
        val assets = ApplicationProvider.getApplicationContext<android.content.Context>().assets
        val packs = PlantPackLoader(AssetPlantPackSource(assets)).load().packs
        assertTrue("This check must use loaded PNG art, not vector fallback", packs.size == 4)
        val renderer = PackPlantRenderer(packs.associate { it.metadata.id to it.images })
        val healthy = renderGardenSnapshot(state, colors, renderer = renderer).asAndroidBitmap()
        val noPlants = renderGardenSnapshot(
            state.copy(plants = emptyList()),
            colors,
            renderer = renderer
        ).asAndroidBitmap()
        val wilted = renderGardenSnapshot(
            state.copy(plants = listOf(state.plants.single().copy(health = 10, condition = PlantCondition.WILTING))),
            colors,
            renderer = renderer
        ).asAndroidBitmap()
        assertFalse(healthy.sameAs(noPlants))
        assertFalse(healthy.sameAs(wilted))
    }

    @Test fun bundledLeafAndSeedColorsFollowTheThemeWithoutChangingTheirShape() {
        val assets = ApplicationProvider.getApplicationContext<android.content.Context>().assets
        val packs = PlantPackLoader(AssetPlantPackSource(assets)).load().packs
        val renderer = PackPlantRenderer(packs.associate { it.metadata.id to it.images })
        fun draw(primary: Color, soil: Color, growth: Int): android.graphics.Bitmap {
            val image = ImageBitmap(256, 256)
            val palette = PlantPalette(primary, primary, primary, primary, Color.White, soil)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(256f, 256f)) {
                renderer.draw(this, Plant("fake", "cactus", growth), palette, PlantVisuals(1f, 1f, 0f))
            }
            return image.asAndroidBitmap()
        }
        val green = draw(Color.Green, Color.Black, 45)
        val blue = draw(Color.Blue, Color.White, 45)
        assertFalse("Changing the accent must change packed plant pixels", green.sameAs(blue))
        val trunk = blue.getPixel(128, 180)
        assertTrue(android.graphics.Color.blue(trunk) > android.graphics.Color.green(trunk))
        val darkSeed = draw(Color.Green, Color.White, 0)
        val lightSeed = draw(Color.Green, Color.Black, 0)
        assertFalse("A seed must stay visible against dark soil", darkSeed.sameAs(lightSeed))
    }
}
