// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.settings

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.screengarden.app.domain.garden.Hemisphere
import com.screengarden.app.ui.theme.ScreenGardenTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EnvironmentControlsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun preferencesHaveLabelledActions() {
        var side = Hemisphere.NORTHERN
        var low = false
        compose.setContent {
            ScreenGardenTheme { EnvironmentControls(side, low, { side = it }, { low = it }) }
        }
        compose.onNodeWithText("Southern hemisphere").performClick()
        compose.onNodeWithContentDescription("Low power visuals").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(Hemisphere.SOUTHERN, side)
            assertEquals(true, low)
        }
    }

    @Test
    fun failedWritesStayVisibleAndSavingDisablesControls() {
        compose.setContent {
            ScreenGardenTheme {
                EnvironmentControls(Hemisphere.NORTHERN, false, { }, { }, saving = true, error = true)
            }
        }
        compose.onNodeWithContentDescription("Low power visuals").assertIsNotEnabled()
        compose.onNodeWithText("Could not save this choice. Try again.").assertExists()
    }
}
