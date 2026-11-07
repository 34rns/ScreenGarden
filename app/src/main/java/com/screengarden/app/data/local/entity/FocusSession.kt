// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A focus session: planned length plus the real start/end instants.
 *
 * [dayStartUtcMillis] is denormalised on purpose. Sessions are read per day
 * (today's list, the garden's history) far more often than they are written, and
 * storing the day key means those queries filter on one indexed column instead
 * of computing bounds from [startedAtUtcMillis] at query time.
 *
 * [endedAtUtcMillis] stays null while a session is running, so an interrupted
 * session is visible on the next launch (Phase 1.3 recovers it).
 */
@Entity(
    tableName = "focus_sessions",
    indices = [Index("dayStartUtcMillis"), Index("plantId")]
)
data class FocusSession(
    @PrimaryKey val sessionId: String,
    val dayStartUtcMillis: Long,
    val startedAtUtcMillis: Long,
    val endedAtUtcMillis: Long? = null,
    val plannedMinutes: Int,
    val wasCompleted: Boolean = false,
    val plantId: String? = null,
    @ColumnInfo(defaultValue = "'OTHER'") val tag: String = "OTHER",
    @ColumnInfo(defaultValue = "'RUNNING'") val status: String = "RUNNING",
    @ColumnInfo(defaultValue = "0") val actualDurationMs: Long = 0,
    @ColumnInfo(defaultValue = "0") val updatedAtUtcMillis: Long = startedAtUtcMillis,
    @ColumnInfo(defaultValue = "-1") val bootCount: Int = -1
)
