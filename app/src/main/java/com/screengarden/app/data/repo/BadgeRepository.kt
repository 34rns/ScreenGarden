// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.data.local.entity.Badge
import kotlinx.coroutines.flow.Flow

/**
 * Earned badges.
 *
 * P18 stores each unlock once; repeated evaluations preserve the earned instant.
 */
interface BadgeRepository {

    fun observeAll(): Flow<List<Badge>>

    suspend fun getBadge(badgeKey: String): Badge?

    suspend fun save(badge: Badge)

    suspend fun delete(badgeKey: String)
}
