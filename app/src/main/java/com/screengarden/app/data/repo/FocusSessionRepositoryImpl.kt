// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import androidx.room.withTransaction
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.data.local.entity.DailyStat
import com.screengarden.app.data.local.entity.FocusSession
import com.screengarden.app.domain.focus.FocusSessionState
import com.screengarden.app.domain.focus.FocusStatus
import com.screengarden.app.domain.focus.FocusTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One running timer, durable checkpoints, and once-only completed-session totals. */
class FocusSessionRepositoryImpl(private val database: ScreenGardenDatabase) : FocusSessionRepository {
    private val dao = database.focusSessionDao()

    override fun observeDay(dayStartUtcMillis: Long): Flow<List<FocusSessionState>> =
        dao.observeDay(dayStartUtcMillis).map { rows -> rows.map { it.toDomain() } }

    override fun observeRunning(): Flow<List<FocusSessionState>> =
        dao.observeRunning().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getSession(sessionId: String): FocusSessionState? = dao.getById(sessionId)?.toDomain()

    override suspend fun activeSession(): FocusSessionState? = dao.activeSession()?.toDomain()

    override suspend fun save(session: FocusSessionState) {
        dao.upsert(session.toEntity())
    }

    override suspend fun delete(sessionId: String) {
        dao.deleteById(sessionId)
    }

    override suspend fun start(session: FocusSessionState): FocusSessionState = database.withTransaction {
        require(session.status == FocusStatus.RUNNING && session.endedAtUtcMillis == null)
        dao.activeSession()?.toDomain() ?: session.also { dao.upsert(it.toEntity()) }
    }

    override suspend fun checkpoint(session: FocusSessionState): Boolean = database.withTransaction {
        val current = dao.getById(session.id)?.toDomain() ?: return@withTransaction false
        if (current.status != FocusStatus.RUNNING || session.actualDurationMs < current.actualDurationMs) {
            return@withTransaction false
        }
        dao.update(session.toEntity())
        if (session.status == FocusStatus.COMPLETED) {
            // Credit completion to its local day, including sessions that cross midnight.
            val day = LocalDay.startOfDay(requireNotNull(session.endedAtUtcMillis))
            val days = database.dailyStatDao()
            val previous = days.getByDay(day) ?: DailyStat(day, updatedAtUtcMillis = session.updatedAtUtcMillis)
            days.upsert(
                previous.copy(
                    focusMinutes = (previous.focusMinutes.toLong() + session.actualDurationMs / 60_000)
                        .coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    sessionsCompleted = (previous.sessionsCompleted.toLong() + 1)
                        .coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    updatedAtUtcMillis = session.updatedAtUtcMillis
                )
            )
        }
        true
    }
}

private fun FocusSession.toDomain(): FocusSessionState = FocusSessionState(
    id = sessionId,
    dayStartUtcMillis = dayStartUtcMillis,
    startedAtUtcMillis = startedAtUtcMillis,
    plannedMinutes = plannedMinutes.coerceIn(1, 240),
    tag = FocusTag.entries.firstOrNull { it.name == tag } ?: FocusTag.OTHER,
    status = FocusStatus.entries.firstOrNull { it.name == status } ?: FocusStatus.CANCELLED,
    endedAtUtcMillis = endedAtUtcMillis,
    actualDurationMs = actualDurationMs.coerceIn(0, plannedMinutes.coerceIn(1, 240) * 60_000L),
    updatedAtUtcMillis = updatedAtUtcMillis,
    bootCount = bootCount,
    plantId = plantId
)

private fun FocusSessionState.toEntity(): FocusSession = FocusSession(
    sessionId = id,
    dayStartUtcMillis = dayStartUtcMillis,
    startedAtUtcMillis = startedAtUtcMillis,
    endedAtUtcMillis = endedAtUtcMillis,
    plannedMinutes = plannedMinutes,
    wasCompleted = status == FocusStatus.COMPLETED,
    plantId = plantId,
    tag = tag.name,
    status = status.name,
    actualDurationMs = actualDurationMs,
    updatedAtUtcMillis = updatedAtUtcMillis,
    bootCount = bootCount
)
