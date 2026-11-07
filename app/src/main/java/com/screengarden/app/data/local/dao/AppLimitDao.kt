// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.screengarden.app.data.local.entity.AppLimit
import kotlinx.coroutines.flow.Flow

/** Reads and writes per-app limits (see [AppLimit]). */
@Dao
interface AppLimitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(limit: AppLimit)

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName")
    suspend fun get(packageName: String): AppLimit?

    @Query("SELECT * FROM app_limits ORDER BY packageName ASC")
    fun observeAll(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits ORDER BY packageName ASC")
    suspend fun getAll(): List<AppLimit>

    /** Only the limits that are currently switched on. */
    @Query("SELECT * FROM app_limits WHERE isEnabled = 1 ORDER BY packageName ASC")
    fun observeEnabled(): Flow<List<AppLimit>>

    @Update
    suspend fun update(limit: AppLimit): Int

    @Query("DELETE FROM app_limits WHERE packageName = :packageName")
    suspend fun delete(packageName: String): Int
}
