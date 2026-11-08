// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.insights

import com.screengarden.app.core.time.LocalDay
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class GardenHistoryTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private fun frame(date: String) = GardenHistoryFrame(
        LocalDay.startOfDay(LocalDate.parse(date), zone),
        1,
        80,
        emptyList()
    )

    @Test fun latestActualSnapshotRepresentsEachCalendarWeekAcrossDst() {
        val old = frame("2026-10-24")
        val newest = frame("2026-10-25")
        val nextWeek = frame("2026-10-26")
        val grouped = GardenHistory.group(listOf(nextWeek, old, newest), HistoryPeriod.WEEK, zone)
        assertEquals(2, grouped.size)
        assertEquals(nextWeek, grouped[0].frame)
        assertEquals(newest, grouped[1].frame)
        assertEquals(LocalDate.of(2026, 10, 19), grouped[1].periodStart)
        assertEquals(2, grouped[1].recordedDays)
    }

    @Test fun monthsHaveNoInventedMissingFrames() {
        val frames = listOf(frame("2026-09-30"), frame("2026-11-01"))
        val grouped = GardenHistory.group(frames, HistoryPeriod.MONTH, zone)
        assertEquals(listOf(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 9, 1)), grouped.map { it.periodStart })
    }
}
