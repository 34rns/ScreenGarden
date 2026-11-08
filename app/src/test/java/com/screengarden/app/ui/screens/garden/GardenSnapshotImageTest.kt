// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import com.screengarden.app.domain.garden.GardenEnvironment
import com.screengarden.app.domain.garden.GardenLight
import com.screengarden.app.domain.garden.Plant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GardenSnapshotImageTest {
    private val colors = lightColorScheme()
    private val garden = GardenUiState(
        loading = false,
        plants = listOf(Plant("fake-flower", "flower", 45, 80)),
        score = 84,
        streakDays = 4
    )

    @Test fun emptyGardenStillExportsAnOpaqueScene() {
        val image = renderGardenSnapshot(garden.copy(plants = emptyList()), colors).asAndroidBitmap()
        assertEquals(255, android.graphics.Color.alpha(image.getPixel(image.width / 2, image.height / 2)))
    }

    @Test fun snapshotUsesTheSuppliedPlantRenderer() {
        val withoutPlant = renderGardenSnapshot(garden.copy(plants = emptyList()), colors).asAndroidBitmap()
        val withPlant = renderGardenSnapshot(
            garden,
            colors,
            renderer = PlantRenderer { scope, _, _, _ -> with(scope) { drawRect(Color.Magenta) } }
        ).asAndroidBitmap()
        assertFalse("A selected renderer must change the exported garden", withoutPlant.sameAs(withPlant))
    }

    @Test fun nightAndOptionalCaptionChangeTheExportedImage() {
        val day = renderGardenSnapshot(garden, colors).asAndroidBitmap()
        val night = renderGardenSnapshot(
            garden.copy(environment = GardenEnvironment(light = GardenLight.NIGHT)),
            colors
        ).asAndroidBitmap()
        assertNotEquals(day.getPixel(100, 100), night.getPixel(100, 100))
        val caption = renderGardenSnapshot(
            garden,
            colors,
            "ScreenGarden\nNov 8, 2026 · 4 counted days"
        ).asAndroidBitmap()
        assertFalse("Caption must be part of the saved image", day.sameAs(caption))
    }
}
