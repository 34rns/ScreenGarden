// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.focus

import android.app.Application
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.R
import com.screengarden.app.domain.focus.FocusSessionState
import com.screengarden.app.domain.focus.FocusTag
import com.screengarden.app.ui.theme.ScreenGardenTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class FocusScreenTest {
    @get:Rule val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun presetsAndTagsReachTheTimerAndCustomRangeIsValidated() {
        var chosen: Pair<Int, FocusTag>? = null
        compose.setContent {
            ScreenGardenTheme {
                FocusScreen(FocusUiState(loading = false), { minutes, tag ->
                    chosen = minutes to tag
                }, { }, { }, { }, { })
            }
        }
        compose.onNodeWithText(app.getString(R.string.focus_duration_minutes, 45)).performScrollTo().performClick()
        compose.onNodeWithText(app.getString(R.string.focus_tag_work)).performScrollTo().performClick()
        compose.onNodeWithText(app.getString(R.string.focus_plant_seed)).performScrollTo().performClick()
        assertEquals(45 to FocusTag.WORK, chosen)
        compose.onNodeWithText(app.getString(R.string.focus_custom)).performScrollTo().performClick()
        compose.onNodeWithTag("focus_custom_minutes").performScrollTo().performTextReplacement("0")
        compose.onNodeWithText(app.getString(R.string.focus_plant_seed)).assertIsNotEnabled()
        compose.onNodeWithTag("focus_custom_minutes").performTextReplacement("240")
        compose.onNodeWithText(app.getString(R.string.focus_plant_seed)).performScrollTo().performClick()
        assertEquals(240 to FocusTag.WORK, chosen)
    }

    @Test
    fun keepScreenOnIsOptionalAndClearsWhenTheTimerEnds() {
        val state = mutableStateOf(
            FocusUiState(
                loading = false,
                active = FocusSessionState("test", 0, 0, 25)
            )
        )
        var view: View? = null
        compose.setContent {
            view = LocalView.current
            ScreenGardenTheme {
                FocusScreen(state.value, { _, _ -> }, { }, { enabled ->
                    state.value = state.value.copy(keepScreenOn = enabled)
                }, { }, { })
            }
        }
        compose.runOnIdle { assertFalse(requireNotNull(view).keepScreenOn) }
        compose.onNodeWithText(app.getString(R.string.focus_keep_screen_on)).performScrollTo().performClick()
        compose.runOnIdle { assertTrue(requireNotNull(view).keepScreenOn) }
        compose.runOnIdle { state.value = state.value.copy(active = null) }
        compose.runOnIdle { assertFalse(requireNotNull(view).keepScreenOn) }
    }

    @Test
    fun runningCountdownAndStopActionAreAccessible() {
        var stopped = 0
        val running = FocusSessionState("test", 0, 0, 25, actualDurationMs = 60_000)
        compose.setContent {
            ScreenGardenTheme {
                FocusScreen(FocusUiState(loading = false, active = running), { _, _ -> }, { stopped++ }, { }, { }, { })
            }
        }
        compose.onNodeWithTag("focus_countdown").assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.focus_stop)).performScrollTo().performClick()
        assertEquals(1, stopped)
    }

    @Test
    fun fullscreenExitKeepsTheSessionAndExplicitStopRemainsAvailable() {
        var stopped = 0
        val running = FocusSessionState("world-session", 0, 0, 25, actualDurationMs = 60_000)
        compose.setContent {
            ScreenGardenTheme {
                FocusScreen(
                    FocusUiState(loading = false, active = running),
                    { _, _ -> },
                    { stopped++ },
                    { },
                    { },
                    { },
                    world = com.screengarden.app.ui.screens.garden.GardenUiState(lowPowerVisuals = true)
                )
            }
        }
        compose.onNodeWithTag("ambient_timer").assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.world_exit)).performClick()
        assertEquals(0, stopped)
        compose.onNodeWithText(app.getString(R.string.world_enter_focus)).performScrollTo().performClick()
        compose.onNodeWithTag("ambient_timer").assertIsDisplayed()
        compose.onNodeWithTag("ambient_stop").performClick()
        assertEquals(1, stopped)
    }
}
