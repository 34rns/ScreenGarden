// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.compose.ui.graphics.asAndroidBitmap
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.model.AccentColor
import com.screengarden.app.domain.model.ThemePreference
import com.screengarden.app.ui.theme.screenGardenColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WallpaperImageTest {
    @Test fun portraitUsesSharedPlantsAndFullBleedSceneWithoutExportCaption() {
        val colors = screenGardenColors(ThemePreference.LIGHT, AccentColor.CALM_GREEN, false)
        val empty = GardenUiState(loading = false)
        val populated = empty.copy(plants = listOf(Plant("fake", "flower", 45, 90, plantedAtUtcMillis = 1)))
        val background = renderGardenSnapshot(
            empty,
            colors,
            width = 360,
            height = 800,
            framed = false
        ).asAndroidBitmap()
        val garden = renderGardenSnapshot(
            populated,
            colors,
            width = 360,
            height = 800,
            framed = false
        ).asAndroidBitmap()
        assertEquals(360, garden.width)
        assertEquals(800, garden.height)
        assertFalse("Saved plants must change the rendered scene", background.sameAs(garden))
        var footerHasPlant = false
        for (y in 640 until 800) {
            for (x in 0 until 360) {
                if (background.getPixel(x, y) != garden.getPixel(x, y)) footerHasPlant = true
            }
        }
        assertFalse("Android controls need an unobstructed lower area", footerHasPlant)
        assertThrows(IllegalArgumentException::class.java) { renderGardenSnapshot(empty, colors, width = 4096) }
    }
}
