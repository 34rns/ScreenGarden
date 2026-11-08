// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.habits

import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.garden.GardenDayRecord
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.domain.garden.PlantStage
import com.screengarden.app.domain.garden.PlantTuning
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class Streaks(val current: Int, val longest: Int)

enum class BadgeKind(val key: String) {
    STREAK_7("streak-7"),
    STREAK_30("streak-30"),
    FOCUS_WEEK_10("focus-week-10"),
    FIRST_BLOOM("first-bloom"),
    FIRST_RECOVERY("first-recovery")
}

/** Calendar-based rules. Rest preserves progress; unknown measurements never count as success. */
object HabitRules {
    fun streaks(days: List<GardenDayRecord>, end: Long, threshold: Int, zone: ZoneId): Streaks {
        require(threshold in 0..100)
        val known = days.filter { it.dayStartUtcMillis <= end }.associateBy { it.dayStartUtcMillis }
        var cursor = end
        var current = 0
        while (true) {
            val day = known[cursor] ?: break
            if (!day.isCompassionDay) {
                if (!qualifies(day, threshold)) break
                current++
            }
            cursor = LocalDay.previousDayStart(cursor, zone)
        }
        var longest = 0
        var run = 0
        var previous: Long? = null
        for (day in known.values.sortedBy { it.dayStartUtcMillis }) {
            if (previous?.let { LocalDay.nextDayStart(it, zone) != day.dayStartUtcMillis } == true) run = 0
            if (!day.isCompassionDay) run = if (qualifies(day, threshold)) run + 1 else 0
            longest = maxOf(longest, run)
            previous = day.dayStartUtcMillis
        }
        return Streaks(current, longest)
    }

    fun qualifies(day: GardenDayRecord, threshold: Int): Boolean =
        day.usageIsBounded && day.score?.let { it >= threshold } == true

    fun eligible(
        days: List<GardenDayRecord>,
        plants: List<Plant>,
        threshold: Int,
        end: Long,
        zone: ZoneId
    ): Set<BadgeKind> {
        val history = days.filter { it.dayStartUtcMillis <= end }
        val longest = streaks(history, end, threshold, zone).longest
        val weeks = history.groupBy {
            val date = Instant.ofEpochMilli(it.dayStartUtcMillis).atZone(zone).toLocalDate()
            LocalDay.startOfDay(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), zone)
        }
        return buildSet {
            if (longest >= 7) add(BadgeKind.STREAK_7)
            if (longest >= 30) add(BadgeKind.STREAK_30)
            if (weeks.values.any { week -> week.sumOf { it.sessionsCompleted.coerceAtLeast(0).toLong() } >= 10 }) {
                add(BadgeKind.FOCUS_WEEK_10)
            }
            addAll(plantBadges(emptyList(), plants))
        }
    }

    fun plantBadges(before: List<Plant>, after: List<Plant>): Set<BadgeKind> = buildSet {
        if (after.any { it.stage == PlantStage.BLOOM }) add(BadgeKind.FIRST_BLOOM)
        val old = before.associateBy { it.id }
        if (after.any {
                it.condition == PlantCondition.RECOVERING ||
                    old[it.id]?.let { previous ->
                        previous.health < PlantTuning.HEALTHY_AT &&
                            it.health > previous.health
                    } ==
                    true
            }
        ) {
            add(BadgeKind.FIRST_RECOVERY)
        }
    }
}
