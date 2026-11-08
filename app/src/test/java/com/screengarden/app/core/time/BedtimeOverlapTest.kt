// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.core.time

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class BedtimeOverlapTest {
    private val night = BedtimeWindow(LocalTime.of(22, 30), LocalTime.of(7, 0))

    @Test fun midnightWindowsCountOnlyTheirIntersection() {
        assertEquals(90L, minutes(night, "2026-11-08T22:00:00Z", "2026-11-09T00:00:00Z"))
        assertEquals(30L, minutes(night, "2026-11-09T06:30:00Z", "2026-11-09T08:00:00Z"))
        assertEquals(0L, minutes(night, "2026-11-09T10:00:00Z", "2026-11-09T11:00:00Z"))
    }

    @Test fun aDisabledWindowCountsNothing() {
        assertEquals(
            0L,
            minutes(
                BedtimeWindow(LocalTime.NOON, LocalTime.NOON),
                "2026-11-08T00:00:00Z",
                "2026-11-10T00:00:00Z"
            )
        )
    }

    @Test fun localClockUsesTheRequestedZone() {
        assertEquals(60L, minutes(night, "2026-11-08T17:00:00Z", "2026-11-08T18:00:00Z", "Asia/Dhaka"))
    }

    @Test fun springClockGapDoesNotInventUsage() {
        assertEquals(
            60L,
            minutes(
                BedtimeWindow(LocalTime.of(1, 30), LocalTime.of(3, 30)),
                "2026-03-29T00:00:00Z",
                "2026-03-29T02:00:00Z",
                "Europe/Berlin"
            )
        )
    }

    @Test fun repeatedAutumnMinutesCountInEachRealInterval() {
        assertEquals(
            60L,
            minutes(
                BedtimeWindow(LocalTime.of(2, 15), LocalTime.of(2, 45)),
                "2026-10-25T00:00:00Z",
                "2026-10-25T02:00:00Z",
                "Europe/Berlin"
            )
        )
    }

    private fun minutes(window: BedtimeWindow, start: String, end: String, zone: String = "UTC"): Long =
        window.overlapMillis(Instant.parse(start).toEpochMilli(), Instant.parse(end).toEpochMilli(), ZoneId.of(zone)) /
            60_000
}
