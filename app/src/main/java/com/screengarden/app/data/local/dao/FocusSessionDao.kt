// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.screengarden.app.data.local.entity.FocusSession
import kotlinx.coroutines.flow.Flow

/** Reads and writes focus sessions (see [FocusSession]). */
@Dao
interface FocusSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: FocusSession)

    @Query("SELECT * FROM focus_sessions WHERE sessionId = :sessionId")
    suspend fun getById(sessionId: String): FocusSession?

    @Query(
        "SELECT * FROM focus_sessions WHERE dayStartUtcMillis = :dayStartUtcMillis " +
            "ORDER BY startedAtUtcMillis ASC"
    )
    fun observeDay(dayStartUtcMillis: Long): Flow<List<FocusSession>>

    @Query(
        "SELECT * FROM focus_sessions WHERE dayStartUtcMillis = :dayStartUtcMillis " +
            "ORDER BY startedAtUtcMillis ASC"
    )
    suspend fun sessionsForDay(dayStartUtcMillis: Long): List<FocusSession>

    /**
     * Sessions with no end instant. A non-empty result after a cold start means
     * the app was killed mid-session and the session needs recovering (Phase 1.3).
     */
    @Query(
        "SELECT * FROM focus_sessions WHERE endedAtUtcMillis IS NULL AND status = 'RUNNING' " +
            "ORDER BY startedAtUtcMillis ASC"
    )
    fun observeRunning(): Flow<List<FocusSession>>

    @Query(
        "SELECT * FROM focus_sessions WHERE endedAtUtcMillis IS NULL AND status = 'RUNNING' " +
            "ORDER BY startedAtUtcMillis ASC LIMIT 1"
    )
    suspend fun activeSession(): FocusSession?

    @Query(
        "SELECT * FROM focus_sessions WHERE dayStartUtcMillis BETWEEN :fromDayUtcMillis AND :toDayUtcMillis " +
            "ORDER BY startedAtUtcMillis DESC"
    )
    fun observeSessions(fromDayUtcMillis: Long, toDayUtcMillis: Long): Flow<List<FocusSession>>

    @Update
    suspend fun update(session: FocusSession): Int

    @Query("DELETE FROM focus_sessions WHERE sessionId = :sessionId")
    suspend fun deleteById(sessionId: String): Int

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()
}
