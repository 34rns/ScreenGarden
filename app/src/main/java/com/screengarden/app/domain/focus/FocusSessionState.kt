// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.focus

/** User-selected labels; persisted names are stable across translations. */
enum class FocusTag { STUDY, WORK, READING, OTHER }

/** A distraction can end softly without destroying any garden progress. */
enum class FocusStatus { RUNNING, COMPLETED, FAILED_SOFT, CANCELLED }

/** Epoch UTC timestamps and duration checkpoints, independent of Android's service. */
data class FocusSessionState(
    val id: String,
    val dayStartUtcMillis: Long,
    val startedAtUtcMillis: Long,
    val plannedMinutes: Int,
    val tag: FocusTag = FocusTag.OTHER,
    val status: FocusStatus = FocusStatus.RUNNING,
    val endedAtUtcMillis: Long? = null,
    val actualDurationMs: Long = 0,
    val updatedAtUtcMillis: Long = startedAtUtcMillis,
    val bootCount: Int = 0,
    val plantId: String? = null
) {
    init {
        require(id.isNotBlank())
        require(plannedMinutes in 1..240)
        require(actualDurationMs in 0..plannedMinutes * 60_000L)
    }

    val plannedDurationMs: Long get() = plannedMinutes * 60_000L
    val remainingDurationMs: Long get() = (plannedDurationMs - actualDurationMs).coerceAtLeast(0)
}

/** Terminal states never restart; elapsed time is supplied from a monotonic runtime clock. */
object FocusSessionMachine {
    fun tick(session: FocusSessionState, actualDurationMs: Long, nowUtcMillis: Long): FocusSessionState {
        if (session.status != FocusStatus.RUNNING) return session
        val actual = actualDurationMs.coerceIn(session.actualDurationMs, session.plannedDurationMs)
        return session.copy(
            actualDurationMs = actual,
            updatedAtUtcMillis = nowUtcMillis,
            status = if (actual == session.plannedDurationMs) FocusStatus.COMPLETED else FocusStatus.RUNNING,
            endedAtUtcMillis = nowUtcMillis.takeIf { actual == session.plannedDurationMs }
        )
    }

    fun end(
        session: FocusSessionState,
        status: FocusStatus,
        actualDurationMs: Long,
        nowUtcMillis: Long
    ): FocusSessionState {
        require(status == FocusStatus.CANCELLED || status == FocusStatus.FAILED_SOFT)
        if (session.status != FocusStatus.RUNNING) return session
        return session.copy(
            status = status,
            actualDurationMs = actualDurationMs.coerceIn(session.actualDurationMs, session.plannedDurationMs),
            updatedAtUtcMillis = nowUtcMillis,
            endedAtUtcMillis = nowUtcMillis
        )
    }

    /** Resume short process gaps; reboot, backward clocks and overdue gaps earn no uncertain reward. */
    fun recover(session: FocusSessionState, nowUtcMillis: Long, bootCount: Int): FocusSessionState {
        if (session.status != FocusStatus.RUNNING) return session
        if (bootCount != session.bootCount || nowUtcMillis < session.updatedAtUtcMillis) {
            return end(session, FocusStatus.CANCELLED, session.actualDurationMs, nowUtcMillis)
        }
        val gap = nowUtcMillis - session.updatedAtUtcMillis
        if (gap < 0 || gap >= session.remainingDurationMs) {
            return end(session, FocusStatus.CANCELLED, session.actualDurationMs, nowUtcMillis)
        }
        return tick(session, session.actualDurationMs + gap, nowUtcMillis)
    }
}
