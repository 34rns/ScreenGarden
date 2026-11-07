// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.focus

import kotlin.math.abs

/** Brief checks are forgiven. Missing permission or unreliable screen events are unknown, not failure. */
object FocusUsePolicy {
    const val GRACE_MS: Long = 30_000
    const val CHECK_INTERVAL_MS: Long = 10_000

    fun shouldSoftFail(otherAppUsageMs: Long?, graceMs: Long = GRACE_MS): Boolean {
        require(graceMs >= 0)
        return otherAppUsageMs != null && otherAppUsageMs > graceMs
    }

    /** Late wakeups exclude later phone use; clock discontinuities make usage timing unknown. */
    fun windowEndUtcMillis(session: FocusSessionState, nowUtcMillis: Long, elapsedMs: Long): Long? {
        if (nowUtcMillis < session.startedAtUtcMillis || elapsedMs < 0) return null
        val wallElapsed = nowUtcMillis - session.startedAtUtcMillis
        if (wallElapsed < 0 || abs(wallElapsed - elapsedMs) > CHECK_INTERVAL_MS) return null
        return session.startedAtUtcMillis + minOf(elapsedMs, session.plannedDurationMs, wallElapsed)
    }
}
