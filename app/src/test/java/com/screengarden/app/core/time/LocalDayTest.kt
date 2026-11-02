// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDayTest {

    /** UTC+6 all year: a stable zone for the ordinary cases. */
    private val dhaka: ZoneId = ZoneId.of("Asia/Dhaka")

    /** Has a DST gap at 02:00 on 2024-03-31. */
    private val berlin: ZoneId = ZoneId.of("Europe/Berlin")

    @Test
    fun lateEveningMapsToTheSameLocalDay() {
        val instant = LocalDate.of(2024, 3, 15).atTime(23, 30).atZone(dhaka).toInstant().toEpochMilli()
        val expected = LocalDate.of(2024, 3, 15).atStartOfDay(dhaka).toInstant().toEpochMilli()

        assertEquals(expected, LocalDay.startOfDay(instant, dhaka))
    }

    @Test
    fun justAfterMidnightMapsToTheNextLocalDay() {
        val instant = LocalDate.of(2024, 3, 16).atTime(0, 30).atZone(dhaka).toInstant().toEpochMilli()
        val expected = LocalDate.of(2024, 3, 16).atStartOfDay(dhaka).toInstant().toEpochMilli()

        assertEquals(expected, LocalDay.startOfDay(instant, dhaka))
    }

    @Test
    fun dayStartDependsOnTheZoneNotOnUtc() {
        val instant = Instant.parse("2024-03-15T11:00:00Z").toEpochMilli()

        assertNotEquals(
            LocalDay.startOfDay(instant, ZoneId.of("UTC")),
            LocalDay.startOfDay(instant, dhaka)
        )
    }

    @Test
    fun startOfDayResolvesTheDaylightSavingGap() {
        val start = LocalDay.startOfDay(LocalDate.of(2024, 3, 31), berlin)

        assertEquals(Instant.parse("2024-03-30T23:00:00Z").toEpochMilli(), start)
    }

    @Test
    fun nextDayStartMovesOneLocalDayAcrossDst() {
        val saturday = LocalDay.startOfDay(LocalDate.of(2024, 3, 30), berlin)

        val sunday = LocalDay.nextDayStart(saturday, berlin)

        assertEquals(LocalDay.startOfDay(LocalDate.of(2024, 3, 31), berlin), sunday)
        assertEquals(saturday, LocalDay.previousDayStart(sunday, berlin))
    }

    @Test
    fun isSameDayComparesLocalDays() {
        val morning = LocalDate.of(2024, 5, 1).atTime(8, 0).atZone(dhaka).toInstant().toEpochMilli()
        val evening = LocalDate.of(2024, 5, 1).atTime(22, 0).atZone(dhaka).toInstant().toEpochMilli()
        val nextMorning = LocalDate.of(2024, 5, 2).atTime(8, 0).atZone(dhaka).toInstant().toEpochMilli()

        assertTrue(LocalDay.isSameDay(morning, evening, dhaka))
        assertFalse(LocalDay.isSameDay(evening, nextMorning, dhaka))
    }
}
