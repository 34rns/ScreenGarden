// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** An actual plant snapshot saved atomically when this completed local day's growth is applied. */
@Entity(tableName = "garden_history")
data class GardenHistory(
    @PrimaryKey val dayStartUtcMillis: Long,
    val capturedAtUtcMillis: Long,
    val score: Int,
    val plantsJson: String
)
