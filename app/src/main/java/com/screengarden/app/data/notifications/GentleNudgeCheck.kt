// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.notifications

import com.screengarden.app.core.time.BedtimeWindow
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.data.usage.DayUsageResult
import com.screengarden.app.domain.limits.AppLimitRule
import com.screengarden.app.domain.notifications.Nudge
import com.screengarden.app.domain.notifications.NudgePolicy
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class NudgePreferences(val enabled: Boolean, val dailyGoalMs: Long, val quietHours: BedtimeWindow?)

/** Null checks app-level permission; a milestone additionally checks its channel. */
interface NudgePublisher {
    fun canPost(nudge: Nudge? = null): Boolean
    fun post(nudge: Nudge): Boolean
}

/** The worker delegates its business rules here so permission, timing and deduplication are testable. */
class GentleNudgeCheck(
    private val preferences: suspend () -> NudgePreferences,
    private val readUsage: suspend (Long) -> DayUsageResult,
    private val readLimits: suspend () -> List<AppLimitRule>,
    private val ledger: NudgeLedger,
    private val publisher: NudgePublisher,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val zoneId: () -> ZoneId = ZoneId::systemDefault
) {
    private val checks = Mutex()

    suspend fun run(): Int = checks.withLock {
        val zone = zoneId()
        if (!canNudge(preferences(), zone) || !publisher.canPost()) return@withLock 0
        val dayKey = LocalDay.startOfDay(nowMillis(), zone)
        val measured = readUsage(dayKey) as? DayUsageResult.Available ?: return@withLock 0
        if (!measured.usage.usageIsBounded || measured.usage.dayStartUtcMillis != dayKey) return@withLock 0
        val limits = readLimits().filter { it.isEnabled }
        val latest = preferences()
        if (!canNudge(latest, zone) || zoneId() != zone || LocalDay.startOfDay(nowMillis(), zone) != dayKey) {
            return@withLock 0
        }
        val crossed = buildList {
            add(NudgePolicy.crossed(measured.usage.totalScreenTimeMillis, latest.dailyGoalMs))
            for (limit in limits) {
                add(
                    NudgePolicy.crossed(
                        measured.usage.app(limit.packageName)?.usageMillis ?: 0,
                        limit.dailyLimitMinutes.toLong() * 60_000,
                        limit.packageName
                    )
                )
            }
        }
        val delivered = ledger.delivered(dayKey)
        var posted = 0
        for (milestones in crossed) {
            if (!canNudge(latest, zone) || zoneId() != zone || LocalDay.startOfDay(nowMillis(), zone) != dayKey) break
            val newest = milestones.lastOrNull { it.key !in delivered } ?: continue
            if (!publisher.canPost(newest)) continue
            if (ledger.claim(dayKey, milestones.map { it.key }.toSet())) {
                if (publisher.post(newest)) posted++
            }
        }
        posted
    }

    private fun canNudge(settings: NudgePreferences, zone: ZoneId): Boolean = settings.enabled &&
        settings.quietHours != null &&
        !settings.quietHours.contains(Instant.ofEpochMilli(nowMillis()).atZone(zone).toLocalTime())
}
