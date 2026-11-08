// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.core.time

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Start-inclusive, end-exclusive local quiet hours; an equal pair disables the window. */
data class BedtimeWindow(val start: LocalTime, val end: LocalTime) {
    fun contains(time: LocalTime): Boolean = when {
        start == end -> false
        start < end -> time >= start && time < end
        else -> time >= start || time < end
    }

    /** Intersects real usage with local quiet hours, splitting at offset changes for DST gaps/repeats. */
    fun overlapMillis(fromMillis: Long, untilMillis: Long, zone: ZoneId): Long {
        if (start == end || untilMillis <= fromMillis) return 0
        var total = 0L
        var cursor = fromMillis
        while (cursor < untilMillis) {
            val instant = Instant.ofEpochMilli(cursor)
            val offset = zone.rules.getOffset(instant)
            val transition = zone.rules.nextTransition(instant)?.instant?.toEpochMilli() ?: untilMillis
            val day = LocalDay.startOfDay(cursor, zone)
            val nextDay = LocalDay.nextDayStart(day, zone)
            val segmentEnd = minOf(untilMillis, transition, nextDay)
            val date = instant.atZone(zone).toLocalDate()
            val localStart = date.atTime(start).toInstant(offset).toEpochMilli()
            val localEnd = date.atTime(end).toInstant(offset).toEpochMilli()
            fun intersection(left: Long, right: Long): Long =
                (minOf(segmentEnd, right) - maxOf(cursor, left)).coerceAtLeast(0L)
            total += if (start < end) {
                intersection(localStart, localEnd)
            } else {
                intersection(day, localEnd) + intersection(localStart, nextDay)
            }
            cursor = segmentEnd
        }
        return total
    }
}
