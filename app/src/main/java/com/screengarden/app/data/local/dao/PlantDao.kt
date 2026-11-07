// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.screengarden.app.data.local.entity.Plant
import kotlinx.coroutines.flow.Flow

/** Reads and writes the garden's plants (see [Plant]). */
@Dao
interface PlantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plant: Plant)

    @Query("SELECT * FROM plants WHERE plantId = :plantId")
    suspend fun getById(plantId: String): Plant?

    /** Oldest planting first, which is the order the garden renders in. */
    @Query("SELECT * FROM plants ORDER BY plantedAtUtcMillis ASC, plantId ASC")
    fun observeAll(): Flow<List<Plant>>

    @Query("SELECT * FROM plants ORDER BY plantedAtUtcMillis ASC, plantId ASC")
    suspend fun getAll(): List<Plant>

    @Query("SELECT COUNT(*) FROM plants")
    suspend fun count(): Int

    @Update
    suspend fun update(plant: Plant): Int

    @Query("DELETE FROM plants WHERE plantId = :plantId")
    suspend fun deleteById(plantId: String): Int
}
