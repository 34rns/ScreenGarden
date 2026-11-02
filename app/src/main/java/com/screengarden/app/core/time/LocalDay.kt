// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The single definition of "which local day does this instant belong to".
 *
 * DECISION (D20): every timestamp stored in Room is epoch milliseconds in UTC.
 * A day is identified by the epoch millis of the *start of the local day* it
 * falls in, computed only through this object. Storing the day as
 * `dayStartUtcMillis` keeps day keys stable for the life of a row (no midnight
 * rollover bugs) and lets every DAO query a day with one indexed column.
 *
 * All functions take the [ZoneId] as the last parameter with the system zone as
 * the default, so tests can pin a zone instead of depending on the machine.
 */
object LocalDay {

    /**
     * Start of the local day that contains [instantMillis], as epoch millis UTC.
     *
     * Uses [LocalDate.atStartOfDay] rather than `minusNanos`, so zones with a
     * midnight DST gap resolve to the first valid instant of that day.
     */
    fun startOfDay(instantMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        startOfDay(Instant.ofEpochMilli(instantMillis).atZone(zoneId).toLocalDate(), zoneId)

    /** Start of the local day for an explicit [date], as epoch millis UTC. */
    fun startOfDay(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zoneId).toInstant().toEpochMilli()

    /** Start of the current local day, as epoch millis UTC. */
    fun todayStart(zoneId: ZoneId = ZoneId.systemDefault()): Long = startOfDay(System.currentTimeMillis(), zoneId)

    /** Start of the local day after [dayStartUtcMillis] (handles DST shifts). */
    fun nextDayStart(dayStartUtcMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val date = Instant.ofEpochMilli(dayStartUtcMillis).atZone(zoneId).toLocalDate()
        return startOfDay(date.plusDays(1), zoneId)
    }

    /** Start of the local day before [dayStartUtcMillis]. */
    fun previousDayStart(dayStartUtcMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val date = Instant.ofEpochMilli(dayStartUtcMillis).atZone(zoneId).toLocalDate()
        return startOfDay(date.minusDays(1), zoneId)
    }

    /** True when both instants fall on the same local day. */
    fun isSameDay(
        firstInstantMillis: Long,
        secondInstantMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean = startOfDay(firstInstantMillis, zoneId) == startOfDay(secondInstantMillis, zoneId)
}
