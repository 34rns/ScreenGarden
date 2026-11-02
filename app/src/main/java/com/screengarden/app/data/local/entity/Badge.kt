// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An unlocked badge. A row exists only once earned, so there is no `isUnlocked`
 * flag to keep in sync with the timestamp.
 *
 * [badgeKey] is a stable string id owned by the domain layer (for example
 * "first_sprout"), not a display name: renaming a badge must not lose progress.
 */
@Entity(tableName = "badges")
data class Badge(@PrimaryKey val badgeKey: String, val unlockedAtUtcMillis: Long)
