// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A committed restore finishes its atomic DataStore edit after failure or process restart. */
@Entity(tableName = "pending_restore")
data class PendingRestore(@PrimaryKey val slot: Int = 1, val settingsJson: String)
