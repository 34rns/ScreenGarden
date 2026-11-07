// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.limits

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.R
import com.screengarden.app.domain.limits.AppLimitRule
import com.screengarden.app.domain.limits.LaunchableApp
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
class AppLimitsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val apps = listOf(LaunchableApp("fake.reader", "Reading"), LaunchableApp("fake.video", "Videos"))

    @Test fun latinCopyAndDurationKeepTheirReadingOrderInRtlLayout() {
        val app: Application = ApplicationProvider.getApplicationContext()
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ScreenGardenTheme {
                    AppLimitsScreen(
                        AppLimitsUiState(apps, listOf(AppLimitRule("fake.video", 30)), loading = false),
                        { _, _, _ -> },
                        { },
                        { }
                    )
                }
            }
        }
        for (text in listOf(app.getString(R.string.limits_copy), app.getString(R.string.limits_duration, 30))) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(
                text
            ).performScrollTo().performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                it(layouts)
            }
            // Content direction must not inherit RTL from the mirrored controls.
            assertEquals(TextDirection.Content, layouts.single().layoutInput.style.textDirection)
            assertEquals(ResolvedTextDirection.Ltr, layouts.single().getParagraphDirection(0))
        }
    }

    @Test fun searchByPackageAndValidateBeforeSaving() {
        var saved: AppLimitRule? = null
        compose.setContent {
            ScreenGardenTheme {
                AppLimitsScreen(AppLimitsUiState(apps = apps, loading = false), { app, min, enabled ->
                    saved = AppLimitRule(app, min, enabled)
                }, { }, { })
            }
        }
        compose.onNodeWithTag("limits_search").performTextReplacement("fake.video")
        compose.onNodeWithText("Reading").assertDoesNotExist()
        compose.onNodeWithText("Videos").assertIsDisplayed()
        compose.onNodeWithTag("edit_fake.video").performScrollTo().performClick()
        compose.onNodeWithTag("limits_minutes").performScrollTo().performTextReplacement("0")
        compose.onNodeWithTag("limits_save").assertIsNotEnabled()
        compose.onNodeWithTag("limits_minutes").performTextReplacement("45")
        compose.onNodeWithTag("limits_save").performScrollTo().performClick()
        assertEquals(AppLimitRule("fake.video", 45), saved)
    }

    @Test fun pauseSwitchKeepsTheChosenMinutes() {
        var saved: AppLimitRule? = null
        compose.setContent {
            ScreenGardenTheme {
                AppLimitsScreen(
                    AppLimitsUiState(apps, listOf(AppLimitRule("fake.video", 30)), loading = false),
                    { app, min, enabled -> saved = AppLimitRule(app, min, enabled) },
                    { },
                    { }
                )
            }
        }
        compose.onNodeWithTag("toggle_fake.video").performScrollTo().performClick()
        assertEquals(AppLimitRule("fake.video", 30, false), saved)
    }
}
