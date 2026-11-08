// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import androidx.room.withTransaction
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.data.local.entity.Badge
import com.screengarden.app.data.local.entity.DailyStat
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.habits.BadgeKind
import com.screengarden.app.domain.habits.CompassionResult
import com.screengarden.app.domain.habits.EarnedBadge
import com.screengarden.app.domain.habits.HabitRules
import com.screengarden.app.domain.habits.HabitsSnapshot
import com.screengarden.app.domain.habits.HabitsStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Local quota and badge writes are atomic; syncing usage cannot erase a rest marker. */
class HabitsRepository(
    private val database: ScreenGardenDatabase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault()
) : HabitsStore {
    override fun observe(): Flow<HabitsSnapshot> = combine(
        database.dailyStatDao().observeRange(Long.MIN_VALUE, Long.MAX_VALUE),
        database.plantDao().observeAll(),
        database.badgeDao().observeAll()
    ) { _, _, _ ->
        database.withTransaction {
            HabitsSnapshot(
                database.dailyStatDao().getRange(Long.MIN_VALUE, LocalDay.startOfDay(nowMillis(), zone))
                    .map { it.toDomain() },
                database.plantDao().getAll().map { it.toDomain() },
                database.badgeDao().getAll().mapNotNull { row ->
                    BadgeKind.entries.firstOrNull {
                        it.key == row.badgeKey
                    }?.let { EarnedBadge(it, row.unlockedAtUtcMillis) }
                }
            )
        }
    }

    override suspend fun setCompassionDay(date: LocalDate, enabled: Boolean, monthlyLimit: Int): CompassionResult =
        database.withTransaction {
            require(monthlyLimit in 0..10)
            val now = nowMillis()
            if (date >
                Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            ) {
                return@withTransaction CompassionResult.FUTURE_DATE
            }
            val days = database.dailyStatDao()
            val key = LocalDay.startOfDay(date, zone)
            val previous = days.getByDay(key)
            if (enabled && previous?.isCompassionDay != true) {
                val monthStart = LocalDay.startOfDay(date.withDayOfMonth(1), zone)
                val monthEnd = LocalDay.startOfDay(date.withDayOfMonth(1).plusMonths(1).minusDays(1), zone)
                if (days.getRange(monthStart, monthEnd).count { it.isCompassionDay } >= monthlyLimit) {
                    return@withTransaction CompassionResult.LIMIT_REACHED
                }
            }
            if (previous == null && enabled) days.insertIfAbsent(DailyStat(key, updatedAtUtcMillis = now))
            days.setCompassionDay(key, enabled)
            CompassionResult.SAVED
        }

    override suspend fun evaluateBadges(threshold: Int) = database.withTransaction {
        val now = nowMillis()
        val eligible = HabitRules.eligible(
            database.dailyStatDao().getRange(Long.MIN_VALUE, LocalDay.startOfDay(now, zone)).map { it.toDomain() },
            database.plantDao().getAll().map { it.toDomain() },
            threshold,
            LocalDay.startOfDay(now, zone),
            zone
        )
        eligible.forEach { database.badgeDao().insertIfAbsent(Badge(it.key, now)) }
    }
}

/** Called inside the garden/session transaction so a brief recovery cannot disappear before the UI observes it. */
internal suspend fun ScreenGardenDatabase.recordPlantBadges(before: List<Plant>, after: List<Plant>, at: Long) {
    HabitRules.plantBadges(before, after).forEach { badgeDao().insertIfAbsent(Badge(it.key, at)) }
}
