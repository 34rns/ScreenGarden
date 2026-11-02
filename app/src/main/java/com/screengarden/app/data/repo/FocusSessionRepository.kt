// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.data.local.entity.FocusSession
import kotlinx.coroutines.flow.Flow

/**
 * Focus sessions, including recovery of a session interrupted by process death.
 *
 * INTERFACE ONLY for now (Task 1.1); Phase 1.3 implements the timer on top of it.
 */
interface FocusSessionRepository {

    fun observeDay(dayStartUtcMillis: Long): Flow<List<FocusSession>>

    /** Sessions still running (no end instant), for crash recovery. */
    fun observeRunning(): Flow<List<FocusSession>>

    suspend fun getSession(sessionId: String): FocusSession?

    suspend fun save(session: FocusSession)

    suspend fun delete(sessionId: String)
}
