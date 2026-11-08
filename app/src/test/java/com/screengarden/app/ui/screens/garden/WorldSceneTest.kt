// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.world.RenderMode
import com.screengarden.app.domain.world.WorldSpace
import com.screengarden.app.domain.world.WorldState
import com.screengarden.app.domain.world.mapWorldPlants
import com.screengarden.app.ui.theme.ScreenGardenTheme
import com.screengarden.app.ui.world.WorldAmbientScreen
import com.screengarden.app.ui.world.WorldScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WorldSceneTest {
    @get:Rule val compose = createComposeRule()

    @Test fun englishDeskClockKeepsItsReadingOrderInRtl() {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ScreenGardenTheme {
                    WorldAmbientScreen(WorldState(lowPowerVisuals = true), RenderMode.DESK, {})
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("ambient_clock")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(ResolvedTextDirection.Ltr, layouts.single().getParagraphDirection(0))
        assertEquals(TextDirection.Content, layouts.single().layoutInput.style.textDirection)
    }

    @Test fun rtlPlantTargetsStayAlignedWithAbsoluteCanvasGeometry() {
        val plants = listOf(Plant("first", "fern", 45), Plant("second", "flower", 45, plantedAtUtcMillis = 1))
        var selected: Plant? = null
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ScreenGardenTheme {
                    WorldScene(
                        WorldState(mapWorldPlants(plants), space = WorldSpace.FIELD, lowPowerVisuals = true),
                        onPlantSelected = { selected = it }
                    )
                }
            }
        }
        val scene = compose.onNodeWithTag("world_canvas").fetchSemanticsNode().boundsInRoot
        val target = compose.onNodeWithTag("garden_plant_second").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Second plant should be right of the first planting in RTL; canvas=$scene target=$target",
            target.center.x > scene.center.x
        )
        compose.onNodeWithTag("garden_plant_second").performClick()
        assertEquals("second", selected?.id)
        compose.onNodeWithContentDescription("Pan right").performClick()
        compose.onNodeWithContentDescription("Return to the first planting").performClick()
        compose.onNodeWithTag("garden_plant_first").assertIsDisplayed()
    }
}
