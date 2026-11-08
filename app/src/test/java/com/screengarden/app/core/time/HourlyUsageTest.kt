// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.core.time

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class HourlyUsageTest {
    private fun millis(text: String) = Instant.parse(text).toEpochMilli()

    @Test fun aSliceIsSplitAtLocalHoursWithFractionalTimezoneOffsets() {
        val bins = LongArray(24)
        HourlyUsage.add(
            bins,
            millis("2026-11-08T02:05:00Z"),
            millis("2026-11-08T03:05:00Z"),
            ZoneId.of("Asia/Kathmandu")
        )
        assertEquals(10 * 60_000L, bins[7])
        assertEquals(50 * 60_000L, bins[8])
        assertEquals(60 * 60_000L, bins.sum())
    }

    @Test fun springGapHasNoInventedHourAndFallRepetitionCountsActualTime() {
        val spring = LongArray(24)
        val zone = ZoneId.of("Europe/Berlin")
        HourlyUsage.add(spring, millis("2026-03-29T00:30:00Z"), millis("2026-03-29T01:30:00Z"), zone)
        assertEquals(30 * 60_000L, spring[1])
        assertEquals(0L, spring[2])
        assertEquals(30 * 60_000L, spring[3])
        val fall = LongArray(24)
        HourlyUsage.add(fall, millis("2026-10-25T00:00:00Z"), millis("2026-10-25T02:00:00Z"), zone)
        assertEquals(120 * 60_000L, fall[2])
    }
}
