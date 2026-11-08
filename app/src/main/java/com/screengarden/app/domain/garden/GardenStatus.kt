// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.habits.HabitRules
import java.time.ZoneId

enum class GardenCoverage { READY, NO_PERMISSION, NO_DATA, INCOMPLETE }

/** Read-only summary for the widget; displaying it never applies garden rewards. */
data class GardenStatus(val score: Int?, val streakDays: Int, val plant: Plant?, val coverage: GardenCoverage)

fun summarizeGardenStatus(
    snapshot: GardenSnapshot,
    today: Long,
    dailyGoalMinutes: Int,
    pickupGoal: Int,
    streakThreshold: Int,
    hasUsageAccess: Boolean,
    zone: ZoneId
): GardenStatus {
    val day = snapshot.days.firstOrNull { it.dayStartUtcMillis == today }
    val coverage = when {
        !hasUsageAccess -> GardenCoverage.NO_PERMISSION
        day == null -> GardenCoverage.NO_DATA
        !day.usageIsBounded -> GardenCoverage.INCOMPLETE
        else -> GardenCoverage.READY
    }
    val score = if (coverage == GardenCoverage.READY && day != null) {
        HealthScoreCalculator.calculate(
            HealthScoreInputs(
                screenMinutes = day.screenMinutes,
                dailyGoalMinutes = dailyGoalMinutes,
                pickups = day.pickups,
                pickupGoal = pickupGoal,
                bedtimeMinutes = day.bedtimeMinutes,
                focusMinutes = day.focusMinutes,
                limitOveragesMinutes = day.limitOveragesMinutes
            )
        )?.value
    } else {
        null
    }
    return GardenStatus(
        score,
        HabitRules.streaks(snapshot.days, LocalDay.previousDayStart(today, zone), streakThreshold, zone).current,
        snapshot.plants.firstOrNull(),
        coverage
    )
}
