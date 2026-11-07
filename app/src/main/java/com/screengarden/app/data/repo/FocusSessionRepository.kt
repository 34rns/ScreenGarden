// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.domain.focus.FocusSessionState
import kotlinx.coroutines.flow.Flow

/**
 * Focus sessions, including recovery of a session interrupted by process death.
 *
 * A session's terminal write and daily focus totals commit together.
 */
interface FocusSessionRepository {

    fun observeDay(dayStartUtcMillis: Long): Flow<List<FocusSessionState>>

    /** Sessions still running (no end instant), for crash recovery. */
    fun observeRunning(): Flow<List<FocusSessionState>>

    suspend fun getSession(sessionId: String): FocusSessionState?

    suspend fun save(session: FocusSessionState)

    suspend fun activeSession(): FocusSessionState?

    /** Returns the existing active session when a second start races the first. */
    suspend fun start(session: FocusSessionState): FocusSessionState

    /** False for a stale checkpoint or an already-ended session. */
    suspend fun checkpoint(session: FocusSessionState): Boolean

    suspend fun delete(sessionId: String)
}
