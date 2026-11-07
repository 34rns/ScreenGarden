// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One plant in the garden.
 *
 * [growthPoints] is the source of size; [stage] mirrors its domain stage for
 * compatibility with existing rows. [condition] keeps recovery independent of size.
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
    val lastFocusAtUtcMillis: Long? = null,
    @ColumnInfo(defaultValue = "0") val growthPoints: Int = 0,
    @ColumnInfo(defaultValue = "'GROWING'") val condition: String = "GROWING"
)
