// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.habits

import com.screengarden.app.domain.garden.GardenDayRecord
import com.screengarden.app.domain.garden.Plant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class EarnedBadge(val kind: BadgeKind, val unlockedAtUtcMillis: Long)
data class HabitsSnapshot(val days: List<GardenDayRecord>, val plants: List<Plant>, val badges: List<EarnedBadge>)
enum class CompassionResult { SAVED, LIMIT_REACHED, FUTURE_DATE }

/** Local milestone and compassion-day boundary; implementations must serialize quota checks and writes. */
interface HabitsStore {
    fun observe(): Flow<HabitsSnapshot>
    suspend fun setCompassionDay(date: LocalDate, enabled: Boolean, monthlyLimit: Int): CompassionResult
    suspend fun evaluateBadges(threshold: Int)
}
