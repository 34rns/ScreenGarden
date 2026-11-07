// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.focus

import kotlinx.coroutines.flow.Flow

/** Presentation reads sessions without depending on Room or the Android timer. */
fun interface FocusStore {
    fun observeSessions(fromDayUtcMillis: Long, toDayUtcMillis: Long): Flow<List<FocusSessionState>>
}

/** Completed focus is credited to its completion day, including overnight sessions. */
data class FocusSummary(val sessionsToday: Int, val minutesThisWeek: Long)

fun summarizeFocus(
    sessions: List<FocusSessionState>,
    todayUtcMillis: Long,
    tomorrowUtcMillis: Long,
    weekUtcMillis: Long,
    nowUtcMillis: Long
): FocusSummary {
    val completed = sessions.filter {
        it.status == FocusStatus.COMPLETED && it.endedAtUtcMillis?.let { end -> end <= nowUtcMillis } == true
    }
    return FocusSummary(
        sessionsToday = completed.count {
            requireNotNull(it.endedAtUtcMillis) in todayUtcMillis until tomorrowUtcMillis
        },
        minutesThisWeek = completed.filter { requireNotNull(it.endedAtUtcMillis) >= weekUtcMillis }
            .sumOf { it.actualDurationMs / 60_000L }
    )
}
