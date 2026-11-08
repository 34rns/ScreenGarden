// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.habits

import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.garden.GardenDayRecord
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitRulesTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val date = LocalDate.of(2026, 10, 30)
    private fun day(offset: Long, score: Int? = 60, rest: Boolean = false, bounded: Boolean = true) = GardenDayRecord(
        LocalDay.startOfDay(date.plusDays(offset), zone), score, 90, 25, 0, 10, 1,
        bounded, true, 1, isCompassionDay = rest
    )

    @Test fun thirtyFiveDaysAreNotTruncatedToTheGardenQueryWindow() {
        val days = (0L..34L).map { day(it) }
        assertEquals(Streaks(35, 35), HabitRules.streaks(days, days.last().dayStartUtcMillis, 60, zone))
        assertTrue(
            BadgeKind.STREAK_30 in HabitRules.eligible(days, emptyList(), 60, days.last().dayStartUtcMillis, zone)
        )
    }

    @Test fun compassionPreservesButDoesNotIncrementAcrossDaylightSavingChange() {
        val days = listOf(day(-7), day(-6, rest = true, score = null, bounded = false), day(-5))
        assertEquals(Streaks(2, 2), HabitRules.streaks(days, days.last().dayStartUtcMillis, 60, zone))
    }

    @Test fun unknownUsageAndMissingCalendarDaysBreakTheStreak() {
        val days = listOf(day(0), day(1, bounded = false), day(2), day(4))
        assertEquals(Streaks(1, 1), HabitRules.streaks(days, day(4).dayStartUtcMillis, 60, zone))
        assertFalse(BadgeKind.STREAK_7 in HabitRules.eligible(days, emptyList(), 60, day(4).dayStartUtcMillis, zone))
    }

    @Test fun thresholdChangesAndFutureRowsAreHandled() {
        val days = listOf(day(0, 70), day(1, 65), day(2, 100))
        assertEquals(Streaks(0, 1), HabitRules.streaks(days, day(1).dayStartUtcMillis, 70, zone))
        assertEquals(Streaks(2, 2), HabitRules.streaks(days, day(1).dayStartUtcMillis, 60, zone))
    }

    @Test fun allCompassionDaysCannotEarnAStreak() {
        val days = (0L..40L).map { day(it, rest = true) }
        assertEquals(Streaks(0, 0), HabitRules.streaks(days, days.last().dayStartUtcMillis, 60, zone))
    }

    @Test fun weeklyFocusCountsCalendarWeeksRatherThanAnySevenDayWindow() {
        val monday = LocalDate.of(2026, 11, 2)
        val first = day(0).copy(dayStartUtcMillis = LocalDay.startOfDay(monday, zone), sessionsCompleted = 5)
        val sunday = first.copy(
            dayStartUtcMillis = LocalDay.startOfDay(monday.plusDays(6), zone),
            sessionsCompleted = 5
        )
        val next = first.copy(dayStartUtcMillis = LocalDay.startOfDay(monday.plusDays(7), zone))
        assertTrue(
            BadgeKind.FOCUS_WEEK_10 in HabitRules.eligible(
                listOf(first, sunday),
                emptyList(),
                60,
                sunday.dayStartUtcMillis,
                zone
            )
        )
        assertFalse(
            BadgeKind.FOCUS_WEEK_10 in HabitRules.eligible(
                listOf(sunday, next),
                emptyList(),
                60,
                next.dayStartUtcMillis,
                zone
            )
        )
    }
}
