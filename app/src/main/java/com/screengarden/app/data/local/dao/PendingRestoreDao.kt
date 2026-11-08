// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.screengarden.app.data.local.entity.PendingRestore

@Dao
interface PendingRestoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: PendingRestore)

    @Query("SELECT * FROM pending_restore WHERE slot = 1")
    suspend fun get(): PendingRestore?

    @Query("DELETE FROM pending_restore")
    suspend fun clear()
}
