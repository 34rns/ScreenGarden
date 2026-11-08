// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.insights

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.R
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.insights.InsightDay
import com.screengarden.app.domain.insights.InsightSummary
import com.screengarden.app.domain.insights.InsightsPeriod
import com.screengarden.app.ui.theme.ScreenGardenTheme
import java.time.LocalDate
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
class InsightsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun numericLeadingCopyAndChartLegendRetainLatinReadingOrderInRtl() {
        val app: Application = ApplicationProvider.getApplicationContext()
        val date = LocalDate.of(2026, 11, 8)
        val summary = InsightSummary(
            listOf(InsightDay(date, LocalDay.startOfDay(date), 60, 80, 3, false)),
            measuredDays = 1,
            totalMinutes = 60,
            pickups = 3,
            averageScore = 80
        )
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ScreenGardenTheme {
                    InsightsScreen(
                        InsightsUiState(
                            loading = false,
                            hasUsageAccess = true,
                            period = InsightsPeriod.DAY,
                            summary = summary
                        ),
                        {},
                        {},
                        {}
                    )
                }
            }
        }
        val texts = listOf(
            app.resources.getQuantityString(R.plurals.insights_coverage, 1, 1, 1),
            app.getString(R.string.insights_goal_legend, app.getString(R.string.insights_time, 3L, 0L))
        )
        for (text in texts) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(
                text
            ).performScrollTo().performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                it(layouts)
            }
            assertEquals(TextDirection.Content, layouts.single().layoutInput.style.textDirection)
            assertEquals(ResolvedTextDirection.Ltr, layouts.single().getParagraphDirection(0))
        }
    }

    @Test fun rangeAndAccessActionsAreLabelledAtLargeTextInRtl() {
        var period = InsightsPeriod.WEEK
        var access = 0
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.3f),
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                ScreenGardenTheme {
                    InsightsScreen(InsightsUiState(loading = false), { period = it }, { }, { access++ })
                }
            }
        }
        compose.onNodeWithText("Month").performClick()
        assertEquals(InsightsPeriod.MONTH, period)
        compose.onNodeWithText("Enable Usage access").performScrollTo().performClick()
        assertEquals(1, access)
    }

    @Test fun readableDailyValuesComplementCanvasCharts() {
        var show by mutableStateOf(InsightsUiState(loading = false, hasUsageAccess = true))
        compose.setContent { ScreenGardenTheme { InsightsScreen(show, {}, {}, {}) } }
        compose.onNodeWithText("Show daily values").performScrollTo().performClick()
        compose.onNodeWithText("Hide daily values").assertIsDisplayed()
        show = show.copy(refreshError = true)
        compose.onNodeWithText(
            "Refresh could not finish. Saved history is still here."
        ).performScrollTo().assertIsDisplayed()
    }
}
