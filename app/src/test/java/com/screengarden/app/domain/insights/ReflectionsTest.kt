// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.insights

import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.garden.GardenDayRecord
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReflectionsTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val today = LocalDate.of(2026, 11, 8)
    private fun day(ago: Long, minutes: Int, score: Int, rest: Boolean = false, bounded: Boolean = true) =
        GardenDayRecord(
            LocalDay.startOfDay(today.minusDays(ago), zone), score, minutes, 25, 1, 20,
            10, bounded, true, 0, bedtimeMinutes = 0, isCompassionDay = rest
        )

    @Test fun gapsAndRestDaysDoNotInflateTimeSavedButKnownFocusStillCounts() {
        val result = Reflections.build(
            InsightsSnapshot(
                listOf(
                    day(0, 60, 82),
                    day(1, 90, 88),
                    day(2, 0, 100, rest = true),
                    day(3, 0, 100, bounded = false),
                    day(8, 0, 100)
                ),
                emptyList()
            ),
            today,
            180,
            80,
            zone
        )
        assertEquals(2, result.measuredDays)
        assertEquals(210L, result.minutesBelowGoal)
        assertEquals(today.minusDays(1), result.bestDate)
        assertEquals(100L, result.focusMinutes)
        assertEquals(ReflectionSuggestion.KEEP_GOING, result.suggestion)
    }

    @Test fun noMeasurementsNeverClaimsSavedTimeOrABestDay() {
        val result = Reflections.build(InsightsSnapshot(emptyList(), emptyList()), today, 180, 80, zone)
        assertNull(result.minutesBelowGoal)
        assertNull(result.bestDate)
        assertEquals(ReflectionSuggestion.BEGIN, result.suggestion)
    }

    @Test fun overGoalUsesSignedNetTimeAndBedtimeSuggestion() {
        val result = Reflections.build(
            InsightsSnapshot(listOf(day(0, 240, 75).copy(bedtimeMinutes = 30)), emptyList()),
            today,
            180,
            80,
            zone
        )
        assertEquals(-60L, result.minutesBelowGoal)
        assertEquals(ReflectionSuggestion.WIND_DOWN, result.suggestion)
    }

    @Test fun historicalBestScoresRemainFrozenAndTiesChooseEarlierDay() {
        val result = Reflections.build(
            InsightsSnapshot(listOf(day(0, 60, 90), day(1, 60, 90)), emptyList()),
            today,
            1,
            1,
            zone
        )
        assertEquals(90, result.bestScore)
        assertEquals(today.minusDays(1), result.bestDate)
    }
}
