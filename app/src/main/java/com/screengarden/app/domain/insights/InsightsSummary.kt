// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.insights

import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.garden.HealthScoreCalculator
import com.screengarden.app.domain.garden.HealthScoreInputs
import java.time.LocalDate
import java.time.ZoneId

data class InsightDay(
    val date: LocalDate,
    val dayStartUtcMillis: Long,
    val minutes: Int?,
    val score: Int?,
    val pickups: Int?,
    val isCompassionDay: Boolean
)
data class TopApp(val packageName: String, val label: String, val usageMillis: Long)
data class InsightSummary(
    val days: List<InsightDay> = emptyList(),
    val measuredDays: Int = 0,
    val totalMinutes: Long? = null,
    val pickups: Long? = null,
    val averageScore: Int? = null,
    val topApps: List<TopApp> = emptyList()
)

/** Missing and rest days stay visible, but never inflate averages or app totals. */
object InsightsSummary {
    fun build(
        snapshot: InsightsSnapshot,
        period: InsightsPeriod,
        today: LocalDate,
        goalMinutes: Int,
        pickupGoal: Int,
        zone: ZoneId
    ): InsightSummary {
        val byDay = snapshot.days.associateBy { it.dayStartUtcMillis }
        val days = (period.dayCount - 1 downTo 0).map { ago ->
            val date = today.minusDays(ago.toLong())
            val key = LocalDay.startOfDay(date, zone)
            val row = byDay[key]
            val measured = row?.usageIsBounded == true && !row.isCompassionDay
            val score = if (!measured) {
                null
            } else if (row!!.gardenApplied) {
                row.score
            } else {
                HealthScoreCalculator.calculate(
                    HealthScoreInputs(
                        screenMinutes = row!!.screenMinutes,
                        dailyGoalMinutes = goalMinutes,
                        pickups = row.pickups,
                        pickupGoal = pickupGoal,
                        bedtimeMinutes = row.bedtimeMinutes,
                        focusMinutes = row.focusMinutes,
                        limitOveragesMinutes = row.limitOveragesMinutes
                    )
                )?.value
            }
            InsightDay(
                date,
                key,
                row?.screenMinutes?.takeIf { measured },
                score,
                row?.pickups?.takeIf { measured },
                row?.isCompassionDay == true
            )
        }
        val measured = days.filter { it.minutes != null }
        val keys = measured.map { it.dayStartUtcMillis }.toSet()
        val scores = measured.mapNotNull { it.score }
        val pickups = measured.mapNotNull { it.pickups }
        val apps = snapshot.apps.filter {
            it.dayStartUtcMillis in keys
        }.groupBy { it.packageName }.map { (name, rows) ->
            TopApp(name, rows.first().label, rows.sumOf { it.usageMillis.coerceAtLeast(0) })
        }.filter {
            it.usageMillis > 0
        }.sortedWith(compareByDescending<TopApp> { it.usageMillis }.thenBy { it.packageName })
        return InsightSummary(
            days,
            measured.size,
            measured.takeIf { it.isNotEmpty() }?.sumOf { it.minutes!!.toLong() },
            pickups.takeIf { it.isNotEmpty() }?.sumOf { it.toLong() },
            scores.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            apps.take(5)
        )
    }
}
