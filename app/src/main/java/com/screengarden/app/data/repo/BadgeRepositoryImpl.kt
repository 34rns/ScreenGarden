// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.data.local.dao.BadgeDao
import com.screengarden.app.data.local.entity.Badge
import kotlinx.coroutines.flow.Flow

/** An earned badge keeps its original unlock instant across repeated evaluations. */
class BadgeRepositoryImpl(private val dao: BadgeDao) : BadgeRepository {
    override fun observeAll(): Flow<List<Badge>> = dao.observeAll()
    override suspend fun getBadge(badgeKey: String): Badge? = dao.get(badgeKey)
    override suspend fun save(badge: Badge) {
        require(badge.badgeKey.isNotBlank() && badge.unlockedAtUtcMillis >= 0)
        dao.insertIfAbsent(badge)
    }
    override suspend fun delete(badgeKey: String) {
        dao.delete(badgeKey)
    }
}
