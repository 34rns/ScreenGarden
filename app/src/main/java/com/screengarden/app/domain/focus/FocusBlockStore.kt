// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.focus

import kotlinx.coroutines.flow.Flow

/** Only user-selected package names are stored; observed accessibility events never are. */
data class FocusBlockPolicy(
    val enabled: Boolean = false,
    val blocked: Set<String> = emptySet(),
    val allowed: Set<String> = emptySet()
) {
    init {
        require(blocked.size <= 256 && allowed.size <= 256)
        require((blocked + allowed).all { it.length <= 200 && PACKAGE.matches(it) })
    }

    fun shouldRedirect(packageName: String?, running: Boolean, protected: Set<String>): Boolean =
        enabled && running && packageName in blocked && packageName !in allowed && packageName !in protected

    private companion object {
        val PACKAGE = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")
    }
}

/** Stale or rebooted timers must not restrict apps after the actual timer disappears. */
fun hasFreshFocus(sessions: List<FocusSessionState>, now: Long, bootCount: Int): Boolean = sessions.any {
    it.status == FocusStatus.RUNNING &&
        bootCount >= 0 &&
        it.bootCount == bootCount &&
        it.remainingDurationMs > 0 &&
        now >= it.updatedAtUtcMillis &&
        now - it.updatedAtUtcMillis <= 5_000
}

interface FocusBlockStore {
    fun observePolicy(): Flow<FocusBlockPolicy>
    suspend fun save(policy: FocusBlockPolicy)
}
