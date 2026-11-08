// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.screengarden.app.data.local.entity.Badge
import kotlinx.coroutines.flow.Flow

/** Reads and writes unlocked badges (see [Badge]). */
@Dao
interface BadgeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(badge: Badge)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(badge: Badge): Long

    @Query("SELECT * FROM badges WHERE badgeKey = :badgeKey")
    suspend fun get(badgeKey: String): Badge?

    /** Most recently unlocked first, which is how the list should render. */
    @Query("SELECT * FROM badges ORDER BY unlockedAtUtcMillis DESC")
    fun observeAll(): Flow<List<Badge>>

    @Query("SELECT * FROM badges ORDER BY unlockedAtUtcMillis DESC")
    suspend fun getAll(): List<Badge>

    @Query("SELECT COUNT(*) FROM badges")
    suspend fun count(): Int

    @Update
    suspend fun update(badge: Badge): Int

    @Query("DELETE FROM badges WHERE badgeKey = :badgeKey")
    suspend fun delete(badgeKey: String): Int
}
