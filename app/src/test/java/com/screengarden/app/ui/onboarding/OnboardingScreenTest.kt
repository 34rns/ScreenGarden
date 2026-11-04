// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.onboarding

import android.Manifest
import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.R
import com.screengarden.app.domain.model.OnboardingChoices
import com.screengarden.app.ui.theme.ScreenGardenTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The first-run happy path, driven through the real composable (P07).
 *
 * These are Compose UI tests on the JVM via Robolectric, so they run inside the
 * normal `testDebugUnitTest` gate instead of needing an emulator (D56). They cover
 * exactly the promises the brief makes: every step is reachable, the usage grant is
 * detected, "not now" is honest, and finishing reports the chosen goals.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp")
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val application: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun denyNotificationPermission() {
        // Pinned so the notification step is always part of the flow on Android 13+.
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun happyPathGrantsUsageSkipsNotificationsAndKeepsTheDefaults() {
        var usageGranted = false
        var notificationsChosen: Boolean? = null
        var completed: OnboardingChoices? = null

        composeRule.setContent {
            ScreenGardenTheme {
                OnboardingScreen(
                    initialChoices = OnboardingChoices(),
                    isUsageAccessGranted = { usageGranted },
                    onOpenUsageAccessSettings = { usageGranted = true },
                    onNotificationsChosen = { notificationsChosen = it },
                    onComplete = { completed = it }
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.onboarding_garden_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_continue)).performClick()

        composeRule.onNodeWithText(text(R.string.onboarding_privacy_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_continue)).performClick()

        composeRule.onNodeWithText(text(R.string.onboarding_usage_not_granted)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_usage_open_settings)).performClick()

        // The grant is detected without leaving the screen: the check runs again right
        // after the settings intent is launched and on every resume (D54).
        composeRule.onNodeWithText(text(R.string.onboarding_usage_granted)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_continue)).performClick()

        composeRule.onNodeWithText(text(R.string.onboarding_notifications_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_notifications_not_now)).performClick()

        composeRule.onNodeWithText(text(R.string.onboarding_goals_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_finish)).performClick()

        assertEquals(false, notificationsChosen)
        assertEquals(OnboardingChoices(), completed)
    }

    @Test
    fun skippingUsageExplainsThatTheGardenStaysEmptyAndStillFinishes() {
        var completed: OnboardingChoices? = null

        composeRule.setContent {
            ScreenGardenTheme {
                OnboardingScreen(
                    initialChoices = OnboardingChoices(),
                    isUsageAccessGranted = { false },
                    onOpenUsageAccessSettings = { },
                    onNotificationsChosen = { },
                    onComplete = { completed = it }
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.onboarding_continue)).performClick()
        composeRule.onNodeWithText(text(R.string.onboarding_continue)).performClick()

        // The explanation may sit below the fold on a short screen, so this asserts it
        // is part of the skip step rather than that it is currently on-screen.
        composeRule.onNodeWithText(text(R.string.onboarding_usage_skip_explanation)).assertExists()
        composeRule.onNodeWithText(text(R.string.onboarding_usage_skip)).performClick()
        // Skipping usage does not skip the optional reminder step.
        composeRule.onNodeWithText(text(R.string.onboarding_notifications_not_now)).performClick()

        composeRule.onNodeWithText(text(R.string.onboarding_goals_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_finish)).performClick()

        assertNotNull(completed)
    }

    @Test
    fun progressIsAnnouncedToScreenReaders() {
        composeRule.setContent {
            ScreenGardenTheme {
                OnboardingScreen(
                    initialChoices = OnboardingChoices(),
                    isUsageAccessGranted = { false },
                    onOpenUsageAccessSettings = { },
                    onNotificationsChosen = { },
                    onComplete = { }
                )
            }
        }

        composeRule.onNodeWithContentDescription(text(R.string.onboarding_progress, 1, 5)).assertIsDisplayed()
    }

    private fun text(@StringRes id: Int, vararg args: Any): String = application.getString(id, *args)
}
