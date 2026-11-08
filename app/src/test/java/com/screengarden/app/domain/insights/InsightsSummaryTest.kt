// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.insights

import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.garden.GardenDayRecord
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InsightsSummaryTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val today = LocalDate.of(2026, 3, 30)
    private fun key(date: LocalDate) = LocalDay.startOfDay(date, zone)
    private fun day(date: LocalDate, minutes: Int, rest: Boolean = false, bounded: Boolean = true) =
        GardenDayRecord(key(date), 80, minutes, 25, 1, 4, 3, bounded, true, key(date), isCompassionDay = rest)

    @Test fun restUnknownAndMissingDaysNeverBecomeZeroUsageOrPerfectScores() {
        val days = listOf(
            day(today, 60),
            day(today.minusDays(1), 500, rest = true),
            day(today.minusDays(2), 900, bounded = false)
        )
        val data = InsightsSnapshot(days, listOf(AppTime(key(today), "reader", "Reading", 60_000)))
        val summary = InsightsSummary.build(data, InsightsPeriod.WEEK, today, 180, 80, zone)
        assertEquals(7, summary.days.size)
        assertEquals(1, summary.measuredDays)
        assertEquals(60L, summary.totalMinutes)
        assertEquals(4L, summary.pickups)
        assertEquals(80, summary.averageScore)
        assertNull(summary.days[4].minutes)
        assertEquals(true, summary.days[5].isCompassionDay)
        assertEquals(60_000L, summary.topApps.single().usageMillis)
    }

    @Test fun currentScoresUseSharedFormulaWhileAppliedScoresStayFrozen() {
        val current = day(today, 180).copy(score = null, gardenApplied = false, focusMinutes = 0)
        val data = InsightsSnapshot(listOf(current, day(today.minusDays(1), 60)), emptyList())
        val summary = InsightsSummary.build(data, InsightsPeriod.WEEK, today, 180, 80, zone)
        assertEquals(80, summary.days[5].score)
        assertEquals(79, summary.days[6].score)
    }

    @Test fun rangesFollowLocalDaysAcrossDstAndAggregateOnlyEligibleApps() {
        val data = InsightsSnapshot(
            listOf(day(today, 1), day(today.minusDays(1), 1, true)),
            listOf(
                AppTime(key(today), "b", "B", 90_000),
                AppTime(key(today), "a", "A", 120_000),
                AppTime(key(today.minusDays(1)), "a", "A", 9_000_000)
            )
        )
        val summary = InsightsSummary.build(data, InsightsPeriod.MONTH, today, 180, 80, zone)
        assertEquals(30, summary.days.size)
        assertEquals(today.minusDays(29), summary.days.first().date)
        assertEquals(listOf("a", "b"), summary.topApps.map { it.packageName })
        assertEquals(120_000L, summary.topApps.first().usageMillis)
    }

    @Test fun emptyHistoryHasUnknownTotalsAndNoScore() {
        val summary = InsightsSummary.build(
            InsightsSnapshot(emptyList(), emptyList()),
            InsightsPeriod.DAY,
            today,
            180,
            80,
            zone
        )
        assertNull(summary.totalMinutes)
        assertNull(summary.pickups)
        assertNull(summary.averageScore)
        assertEquals(0, summary.measuredDays)
    }
}
