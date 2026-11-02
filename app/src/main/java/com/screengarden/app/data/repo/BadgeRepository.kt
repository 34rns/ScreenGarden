// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.data.local.entity.Badge
import kotlinx.coroutines.flow.Flow

/**
 * Earned badges.
 *
 * INTERFACE ONLY for now (Task 1.1); unlocking rules arrive with the growth use
 * cases in Phase 1.5.
 */
interface BadgeRepository {

    fun observeAll(): Flow<List<Badge>>

    suspend fun getBadge(badgeKey: String): Badge?

    suspend fun save(badge: Badge)

    suspend fun delete(badgeKey: String)
}
