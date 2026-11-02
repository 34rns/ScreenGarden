// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One plant in the garden.
 *
 * [stage] is stored as an Int rather than an enum so a new growth stage can be
 * added later without a TypeConverter or a schema change that rewrites every
 * row. The domain layer maps it to a sealed/enum type.
 *
 * [plantId] is a client-generated UUID string: the garden is created on device
 * with no server, so there is no authority to hand out integer ids.
 */
@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey val plantId: String,
    val species: String,
    val stage: Int = 0,
    val health: Int = 100,
    val plantedAtUtcMillis: Long,
    val lastFocusAtUtcMillis: Long? = null
)
