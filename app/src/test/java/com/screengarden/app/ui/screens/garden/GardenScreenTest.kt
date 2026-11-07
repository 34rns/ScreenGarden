// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.R
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.ui.theme.ScreenGardenTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real Compose interactions on the JVM; platform usage-access round trips need an emulator. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GardenScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val application: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun missingPermissionKeepsScoreUnknownAndBothActionsWork() {
        var grants = 0
        var starts = 0
        composeRule.setContent {
            ScreenGardenTheme {
                GardenScreen(
                    state = GardenUiState(loading = false, availability = GardenAvailability.NO_PERMISSION),
                    onStartFocus = { starts++ },
                    onGrantUsage = { grants++ },
                    onRetry = { }
                )
            }
        }

        composeRule.onNodeWithContentDescription(text(R.string.garden_score_unavailable)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.garden_score_description, 0)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(text(R.string.garden_score_description, 100)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.garden_grant_usage)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.garden_start_focus)).performScrollTo().performClick()
        assertEquals(1, grants)
        assertEquals(1, starts)
    }

    @Test
    fun plantOpensItsStageHealthDateAndDismissibleDetails() {
        show(GardenUiState(false, GardenAvailability.READY, listOf(flower), score = 84, streakDays = 4))

        composeRule.onNodeWithContentDescription(text(R.string.garden_score_description, 84)).assertIsDisplayed()
        composeRule.onNodeWithTag("garden_plant_flower").performClick()
        composeRule.onNodeWithText(text(R.string.garden_detail_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.garden_stage_bloom)).assertExists()
        composeRule.onNodeWithText(text(R.string.garden_detail_health_value, 90)).assertExists()
        composeRule.onNodeWithText(text(R.string.garden_detail_planted)).assertExists()
        composeRule.onNodeWithText(text(R.string.garden_date_unknown)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.garden_detail_close)).performScrollTo().performClick()
        composeRule.onNodeWithTag("garden_plant_details").assertDoesNotExist()
    }

    @Test
    fun incompleteUsageRetainsSavedPlantsAndCanRefresh() {
        var retries = 0
        composeRule.setContent {
            ScreenGardenTheme {
                GardenScreen(
                    state = GardenUiState(false, GardenAvailability.INCOMPLETE_USAGE, listOf(flower)),
                    onStartFocus = { },
                    onGrantUsage = { },
                    onRetry = { retries++ }
                )
            }
        }

        composeRule.onNodeWithTag("garden_plant_flower").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.garden_score_unavailable)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.garden_incomplete_body)).assertExists()
        composeRule.onNodeWithText(text(R.string.garden_refresh)).performScrollTo().performClick()
        assertEquals(1, retries)
    }

    @Test
    fun refreshErrorShowsRetryWithoutRemovingSavedPlant() {
        var retries = 0
        composeRule.setContent {
            ScreenGardenTheme {
                GardenScreen(
                    state = GardenUiState(false, GardenAvailability.READY, listOf(flower), error = true),
                    onStartFocus = { },
                    onGrantUsage = { },
                    onRetry = { retries++ }
                )
            }
        }

        composeRule.onNodeWithTag("garden_plant_flower").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.garden_error_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.garden_retry)).performScrollTo().performClick()
        assertEquals(1, retries)
    }

    @Test
    fun unknownSpeciesUsesSafeRendererAndHonestMissingDate() {
        val unknownPlant = Plant("unknown", "future-pack-species", growthPoints = 15)
        show(GardenUiState(false, GardenAvailability.READY, listOf(unknownPlant)))

        val description = text(
            R.string.garden_plant_description,
            text(R.string.garden_plant_unknown),
            text(R.string.garden_stage_sprout)
        )
        composeRule.onNodeWithContentDescription(description).performClick()
        composeRule.onNodeWithText(text(R.string.garden_date_unknown)).assertExists()
        composeRule.onNodeWithText(text(R.string.garden_detail_close)).performScrollTo().performClick()
    }

    @Test
    fun loadingShowsProgressAndDoesNotPretendDataIsEmpty() {
        show(GardenUiState())

        composeRule.onNodeWithTag("garden_loading").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.garden_loading)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.garden_empty_title)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.garden_grant_usage)).assertDoesNotExist()
    }

    @Test
    fun emptyHistoryHasAnExplanationAndFocusRemainsReachable() {
        show(GardenUiState(loading = false))

        composeRule.onNodeWithText(text(R.string.garden_empty_body)).assertExists()
        composeRule.onNodeWithText(text(R.string.garden_start_focus)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun rendererCanBeReplacedWithoutChangingPlantInteraction() {
        val draws = AtomicInteger()
        var view: View? = null
        composeRule.setContent {
            view = LocalView.current
            ScreenGardenTheme {
                CompositionLocalProvider(
                    LocalPlantRenderer provides PlantRenderer { _, _, _, _ -> draws.incrementAndGet() }
                ) {
                    GardenScreen(
                        GardenUiState(false, GardenAvailability.READY, listOf(flower)),
                        { },
                        { },
                        { }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("garden_plant_flower").assertIsDisplayed().performClick()
        composeRule.onNodeWithText(text(R.string.garden_detail_title)).assertIsDisplayed()
        // Robolectric drives layout but does not guarantee a draw traversal.
        // Draw into memory only; public screenshots still come from the emulator.
        composeRule.runOnIdle {
            val root = checkNotNull(view)
            val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            bitmap.recycle()
        }
        assertTrue(draws.get() > 0)
    }

    @Test
    fun largeTextAndRtlKeepPlantTargetsAndFocusReachable() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.3f),
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                ScreenGardenTheme {
                    GardenScreen(
                        GardenUiState(false, GardenAvailability.READY, listOf(flower, recoveringFern), score = 84),
                        { },
                        { },
                        { }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("garden_plant_flower").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("garden_plant_fern").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.garden_start_focus)).performScrollTo().assertIsDisplayed()
    }

    private fun show(state: GardenUiState) {
        composeRule.setContent { ScreenGardenTheme { GardenScreen(state, { }, { }, { }) } }
    }

    private fun text(@StringRes id: Int, vararg args: Any): String = application.getString(id, *args)

    private val flower = Plant("flower", "flower", growthPoints = 45, health = 90, plantedAtUtcMillis = 1793318400000L)
    private val recoveringFern = Plant(
        "fern",
        "fern",
        growthPoints = 15,
        health = 44,
        condition = PlantCondition.RECOVERING
    )
}
