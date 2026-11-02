// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A per-app daily limit. One row per app, so the package name is the key.
 *
 * [isEnabled] exists so a limit can be paused without losing its value.
 */
@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey val packageName: String,
    val dailyLimitMinutes: Int,
    val isEnabled: Boolean = true,
    val updatedAtUtcMillis: Long
)
