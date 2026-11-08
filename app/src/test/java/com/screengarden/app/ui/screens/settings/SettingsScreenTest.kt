// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.screengarden.app.domain.model.AccentColor
import com.screengarden.app.domain.model.ThemePreference
import com.screengarden.app.ui.theme.ScreenGardenTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsScreenTest {
    @Test fun chargingOffersDeskWithoutStartingItAndManualExitWorks() {
        val state = mutableStateOf(SettingsUiState(loading = false))
        val charging = mutableStateOf(false)
        compose.setContent {
            ScreenGardenTheme {
                SettingsScreen(
                    {},
                    state = state.value,
                    actions = SettingsActions(setDeskMode = { state.value = state.value.copy(deskModeEnabled = it) }),
                    world = com.screengarden.app.ui.screens.garden.GardenUiState(lowPowerVisuals = true),
                    charging = charging.value
                )
            }
        }
        compose.onNodeWithText("Start Desk mode").assertDoesNotExist()
        compose.onNodeWithContentDescription("Enable Desk mode").performScrollTo().performClick()
        compose.onNodeWithText("Start Desk mode").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { charging.value = true }
        compose.onNodeWithTag("world_ambient").assertDoesNotExist()
        compose.onNodeWithText("Charging · Start Desk mode").performScrollTo().performClick()
        compose.onNodeWithTag("world_ambient").assertIsDisplayed()
        compose.onNodeWithText("Exit view").performClick()
        compose.onNodeWithTag("world_ambient").assertDoesNotExist()
    }

    @get:Rule val compose = createComposeRule()

    @Test fun sceneryParagraphsKeepEnglishReadingOrderInMirroredSettings() {
        compose.setContent {
            ScreenGardenTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SettingsScreen({}, state = SettingsUiState(loading = false))
                }
            }
        }
        for (text in listOf(
            "Seasons follow your device date and hemisphere. Weather is decorative, created on your device.",
            "Keep the scenery still and hide particles. Your plants grow as usual."
        )) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text).performScrollTo()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(ResolvedTextDirection.Ltr, layouts.single().getParagraphDirection(0))
            assertEquals(TextDirection.Content, layouts.single().layoutInput.style.textDirection)
        }
    }

    @Test fun themeAndAccentAreLabelledAndInvokeStoredChoiceActions() {
        var theme = ThemePreference.SYSTEM
        var accent = AccentColor.CALM_GREEN
        compose.setContent {
            ScreenGardenTheme {
                Box(Modifier.height(900.dp)) {
                    SettingsScreen(
                        {},
                        state = SettingsUiState(loading = false, theme = ThemePreference.AMOLED),
                        actions = SettingsActions(setTheme = { theme = it }, setAccent = { accent = it })
                    )
                }
            }
        }
        compose.onNodeWithText("AMOLED").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Dark").performClick()
        compose.onNodeWithText("Heather").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(ThemePreference.DARK, theme)
            assertEquals(AccentColor.HEATHER, accent)
        }
    }

    @Test fun failedWritesRemainVisibleAndSavingBlocksFurtherChoices() {
        compose.setContent {
            ScreenGardenTheme {
                SettingsScreen({}, state = SettingsUiState(loading = false, saving = true, error = true))
            }
        }
        compose.onNodeWithText("Could not save this choice. Try again.").assertExists()
        compose.onNodeWithText("AMOLED").performScrollTo().assertIsNotEnabled()
    }

    @Test fun introductionNeedsConfirmationAndCancelKeepsExistingState() {
        var restarted = false
        compose.setContent {
            ScreenGardenTheme {
                SettingsScreen(
                    {},
                    state = SettingsUiState(loading = false),
                    actions = SettingsActions(showIntroduction = { restarted = true })
                )
            }
        }
        compose.onNodeWithText("Show introduction again").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertFalse(restarted) }
    }
}
