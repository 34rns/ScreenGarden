// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

import com.screengarden.app.core.time.LocalDay
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GardenStatusTest {
    private val zone = ZoneId.of("UTC")
    private val today = Instant.parse("2026-11-08T00:00:00Z").toEpochMilli()
    private fun day(key: Long, bounded: Boolean = true, score: Int? = 80) = GardenDayRecord(
        key, score, 120, 30, 1, 30, 2, bounded, key != today, today,
        bedtimeMinutes = 0
    )

    @Test fun missingPermissionNeverClaimsTodayScoreButKeepsSavedPlantsAndStreak() {
        val plant = Plant("fake", "flower", 45, 70, plantedAtUtcMillis = today)
        val snapshot = GardenSnapshot(listOf(plant), listOf(day(LocalDay.previousDayStart(today, zone)), day(today)))
        val status = summarizeGardenStatus(snapshot, today, 180, 80, 60, false, zone)
        assertNull(status.score)
        assertEquals(plant, status.plant)
        assertEquals(1, status.streakDays)
        assertEquals(GardenCoverage.NO_PERMISSION, status.coverage)
    }

    @Test fun emptyAndUnboundedDaysRemainUnknown() {
        val empty = summarizeGardenStatus(GardenSnapshot(emptyList(), emptyList()), today, 180, 80, 60, true, zone)
        assertNull(empty.score)
        assertEquals(GardenCoverage.NO_DATA, empty.coverage)
        val partial = summarizeGardenStatus(
            GardenSnapshot(emptyList(), listOf(day(today, false))),
            today,
            180,
            80,
            60,
            true,
            zone
        )
        assertNull(partial.score)
        assertEquals(GardenCoverage.INCOMPLETE, partial.coverage)
    }

    @Test fun boundedTodayUsesTheExistingCalculatorAndCurrentGoals() {
        val snapshot = GardenSnapshot(emptyList(), listOf(day(today)))
        val status = summarizeGardenStatus(snapshot, today, 180, 80, 60, true, zone)
        val expected = HealthScoreCalculator.calculate(
            HealthScoreInputs(120, 180, 30, 80, bedtimeMinutes = 0, focusMinutes = 30)
        )?.value
        assertEquals(expected, status.score)
        assertEquals(GardenCoverage.READY, status.coverage)
    }
}
