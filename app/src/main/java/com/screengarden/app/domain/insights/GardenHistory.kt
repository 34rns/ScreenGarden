// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.insights

import com.screengarden.app.domain.garden.Plant
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.Flow

data class GardenHistoryFrame(
    val dayStartUtcMillis: Long,
    val capturedAtUtcMillis: Long,
    val score: Int,
    val plants: List<Plant>
)
data class GardenHistorySnapshot(val frames: List<GardenHistoryFrame> = emptyList(), val unreadableFrames: Int = 0)
enum class HistoryPeriod { WEEK, MONTH }
data class GardenHistoryEntry(val periodStart: LocalDate, val frame: GardenHistoryFrame, val recordedDays: Int)

interface GardenHistoryStore {
    fun observe(): Flow<GardenHistorySnapshot>
}

/** Latest recorded frame in each calendar period, newest first; missing periods are not invented. */
object GardenHistory {
    fun group(frames: List<GardenHistoryFrame>, period: HistoryPeriod, zone: ZoneId): List<GardenHistoryEntry> =
        frames.groupBy { frame ->
            val date = Instant.ofEpochMilli(frame.dayStartUtcMillis).atZone(zone).toLocalDate()
            when (period) {
                HistoryPeriod.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                HistoryPeriod.MONTH -> date.withDayOfMonth(1)
            }
        }.map { (date, group) ->
            GardenHistoryEntry(date, group.maxBy { it.dayStartUtcMillis }, group.size)
        }.sortedByDescending { it.periodStart }
}
