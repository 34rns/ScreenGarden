// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.habits

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.R
import com.screengarden.app.ui.theme.ScreenGardenTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HabitsScreenTest {
    @Test fun largeTextAndRtlRetainLabelledThresholdAndAllowanceControls() {
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.3f),
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                ScreenGardenTheme { HabitsScreen(HabitsUiState(loading = false), { _, _ -> }, { }, { }, { }) }
            }
        }
        compose.onNodeWithContentDescription(
            application.getString(R.string.habits_threshold, 60)
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(
            application.getString(R.string.habits_allowance, 3)
        ).performScrollTo().assertIsDisplayed()
    }

    @get:Rule val compose = createComposeRule()
    private val application: Application = ApplicationProvider.getApplicationContext()
    private fun text(id: Int) = application.getString(id)

    @Test fun pastDateActionAndBackAreAccessibleAlongsideUnclaimedBadges() {
        var chosen = ""
        var back = 0
        compose.setContent {
            ScreenGardenTheme {
                HabitsScreen(HabitsUiState(loading = false), { date, enabled ->
                    if (enabled) chosen = date
                }, { }, { }, { back++ })
            }
        }
        compose.onNodeWithText(text(R.string.habits_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.habits_back)).performClick()
        compose.onNodeWithText(text(R.string.habits_date)).performScrollTo().performTextReplacement("2026-10-25")
        compose.onNodeWithText(text(R.string.habits_mark)).performScrollTo().performClick()
        assertEquals("2026-10-25", chosen)
        assertEquals(1, back)
    }

    @Test fun invalidDateFeedbackRemainsVisible() {
        compose.setContent {
            ScreenGardenTheme {
                HabitsScreen(HabitsUiState(loading = false, message = HabitsMessage.INVALID_DATE), { _, _ ->
                }, { }, { }, { })
            }
        }
        compose.onNodeWithText(text(R.string.habits_invalid_date)).performScrollTo().assertIsDisplayed()
    }
}
