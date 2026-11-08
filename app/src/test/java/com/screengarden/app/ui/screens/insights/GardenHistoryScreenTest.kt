// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.insights

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.insights.GardenHistoryEntry
import com.screengarden.app.domain.insights.GardenHistoryFrame
import com.screengarden.app.domain.insights.HistoryPeriod
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
class GardenHistoryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun actualSnapshotAndNumericCopyStayAccessibleInLargeRtlLayout() {
        val date = LocalDate.of(2026, 11, 2)
        val frame = GardenHistoryFrame(
            LocalDay.startOfDay(date),
            LocalDay.startOfDay(date.plusDays(1)),
            80,
            listOf(Plant("a", "flower", 45, 80))
        )
        var period = HistoryPeriod.WEEK
        var back = 0
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.3f),
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                ScreenGardenTheme {
                    GardenHistoryScreen(
                        GardenHistoryUiState(
                            loading = false,
                            entries = listOf(GardenHistoryEntry(date, frame, 1))
                        ),
                        { period = it },
                        {},
                        { back++ }
                    )
                }
            }
        }
        compose.onNodeWithText("Months").performClick()
        assertEquals(HistoryPeriod.MONTH, period)
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("1 saved day. Latest score: 80 / 100.").performScrollTo()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(ResolvedTextDirection.Ltr, layouts.single().getParagraphDirection(0))
        compose.onNodeWithText("Back to Insights").performScrollTo().performClick()
        assertEquals(1, back)
    }
}
