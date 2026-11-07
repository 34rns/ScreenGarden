// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per local day, aggregating that day's screen time and focus work.
 *
 * Keyed by [dayStartUtcMillis], the start of the local day (see
 * [com.screengarden.app.core.time.LocalDay]); never store a wall-clock date
 * string here, or day boundaries would break when the user travels.
 */
@Entity(tableName = "daily_stats")
data class DailyStat(
    @PrimaryKey val dayStartUtcMillis: Long,
    val focusMinutes: Int = 0,
    val screenMinutes: Int = 0,
    val sessionsCompleted: Int = 0,
    val plantsGrown: Int = 0,
    val updatedAtUtcMillis: Long,
    val score: Int? = null,
    val pickups: Int? = null,
    val unlocks: Int? = null,
    /** False for migrated rows until the bounded measurement replaces their usage. */
    @ColumnInfo(defaultValue = "0") val usageIsBounded: Boolean = false,
    @ColumnInfo(defaultValue = "0") val gardenApplied: Boolean = false
)
