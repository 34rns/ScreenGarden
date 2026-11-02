// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Screen time for one app on one local day.
 *
 * The composite primary key means re-collecting usage for the same day and app
 * is an upsert, not a duplicate row. [packageName] is indexed separately so
 * "usage of this app over the last 30 days" stays cheap.
 */
@Entity(
    tableName = "app_usage_daily",
    primaryKeys = ["dayStartUtcMillis", "packageName"],
    indices = [Index("packageName")]
)
data class AppUsageDaily(
    val dayStartUtcMillis: Long,
    val packageName: String,
    val usageMillis: Long = 0L,
    val launchCount: Int = 0,
    val updatedAtUtcMillis: Long
)
