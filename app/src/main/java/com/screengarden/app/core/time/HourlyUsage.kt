// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.core.time

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Splits measured real time by local clock hour; repeated DST hours share a bin. */
object HourlyUsage {
    fun add(bins: LongArray, fromMillis: Long, untilMillis: Long, zone: ZoneId) {
        require(bins.size == 24)
        var cursor = fromMillis
        while (cursor < untilMillis) {
            val local = Instant.ofEpochMilli(cursor).atZone(zone)
            val next = local.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli()
            val transition = zone.rules.nextTransition(Instant.ofEpochMilli(cursor))?.instant?.toEpochMilli()
            val end = minOf(untilMillis, next, transition ?: Long.MAX_VALUE)
            check(end > cursor)
            bins[local.hour] += end - cursor
            cursor = end
        }
    }
}
