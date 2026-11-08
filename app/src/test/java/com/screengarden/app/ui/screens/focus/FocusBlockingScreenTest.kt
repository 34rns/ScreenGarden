// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.focus

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.screengarden.app.domain.focus.FocusBlockPolicy
import com.screengarden.app.domain.limits.LaunchableApp
import com.screengarden.app.ui.theme.ScreenGardenTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FocusBlockingScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun permissionExplanationPrecedesAndroidSettingsAndListsDoNotEnableBlocking() {
        var opened = 0
        var saved = FocusBlockPolicy()
        compose.setContent {
            ScreenGardenTheme {
                FocusBlockingScreen(
                    FocusBlockingUiState(apps = listOf(LaunchableApp("demo.video", "Video app")), loading = false),
                    false,
                    { saved = it },
                    { opened++ },
                    {},
                    {}
                )
            }
        }
        compose.onNodeWithContentDescription("Block selected apps during focus").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Video app: Block").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(setOf("demo.video"), saved.blocked)
            assertFalse(saved.enabled)
            assertEquals(0, opened)
        }
        compose.onNodeWithText("Review the Android permission").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, opened) }
        compose.onNodeWithText("Open Android accessibility settings").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }
}
