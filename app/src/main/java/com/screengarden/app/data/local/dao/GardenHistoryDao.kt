// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.screengarden.app.data.local.entity.GardenHistory
import kotlinx.coroutines.flow.Flow

/** Snapshots are insert-once; later usage reads and settings cannot redraw the past. */
@Dao
interface GardenHistoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(frame: GardenHistory): Long

    @Query("SELECT * FROM garden_history ORDER BY dayStartUtcMillis DESC")
    fun observeAll(): Flow<List<GardenHistory>>

    @Query("SELECT * FROM garden_history ORDER BY dayStartUtcMillis ASC")
    suspend fun getAll(): List<GardenHistory>

    @Query("DELETE FROM garden_history")
    suspend fun deleteAll()
}
