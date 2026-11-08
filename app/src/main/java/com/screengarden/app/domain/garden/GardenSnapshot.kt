// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

/** Consistent persisted plants and requested day records, ordered oldest first. */
data class GardenSnapshot(val plants: List<Plant>, val days: List<GardenDayRecord>)

/** Nullable usage counters and score mean unknown; bounded usage must be checked before scoring. */
data class GardenDayRecord(
    val dayStartUtcMillis: Long,
    val score: Int?,
    val screenMinutes: Int,
    val focusMinutes: Int,
    val sessionsCompleted: Int,
    val pickups: Int?,
    val unlocks: Int?,
    val usageIsBounded: Boolean,
    val gardenApplied: Boolean,
    val updatedAtUtcMillis: Long,
    val limitOveragesMinutes: List<Int>? = null,
    val bedtimeMinutes: Int? = null
)
