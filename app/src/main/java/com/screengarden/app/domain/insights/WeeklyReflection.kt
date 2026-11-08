// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.insights

import java.time.LocalDate
import java.time.ZoneId

enum class ReflectionSuggestion { BEGIN, WIND_DOWN, PAUSE, FOCUS, KEEP_GOING }

data class WeeklyReflection(
    val measuredDays: Int = 0,
    val minutesBelowGoal: Long? = null,
    val bestDate: LocalDate? = null,
    val bestScore: Int? = null,
    val focusMinutes: Long = 0,
    val suggestion: ReflectionSuggestion = ReflectionSuggestion.BEGIN
)

/** Templates receive only known local measurements; rest days never count as saved time. */
object Reflections {
    fun build(
        snapshot: InsightsSnapshot,
        today: LocalDate,
        goal: Int,
        pickupGoal: Int,
        zone: ZoneId
    ): WeeklyReflection {
        val summary = InsightsSummary.build(snapshot, InsightsPeriod.WEEK, today, goal, pickupGoal, zone)
        val keys = summary.days.map { it.dayStartUtcMillis }.toSet()
        val known = summary.days.filter { it.minutes != null }
        val measuredKeys = known.map { it.dayStartUtcMillis }.toSet()
        val rows = snapshot.days.filter { it.dayStartUtcMillis in keys }
        val measured = rows.filter { it.dayStartUtcMillis in measuredKeys }
        val focus = rows.sumOf { it.focusMinutes.coerceAtLeast(0).toLong() }
        val best = known.filter { it.score != null }.sortedWith(
            compareByDescending<InsightDay> { it.score }.thenBy { it.date }
        ).firstOrNull()
        val saved = summary.totalMinutes?.let { known.size * goal.toLong() - it }
        val suggestion = when {
            known.isEmpty() -> ReflectionSuggestion.BEGIN
            measured.any { (it.bedtimeMinutes ?: 0) > 15 } -> ReflectionSuggestion.WIND_DOWN
            saved != null && saved < 0 || measured.any { (it.pickups ?: 0) > pickupGoal } -> ReflectionSuggestion.PAUSE
            focus == 0L -> ReflectionSuggestion.FOCUS
            else -> ReflectionSuggestion.KEEP_GOING
        }
        return WeeklyReflection(known.size, saved, best?.date, best?.score, focus, suggestion)
    }
}
