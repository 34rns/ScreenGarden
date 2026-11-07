// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.data.local.dao.AppLimitDao
import com.screengarden.app.data.local.entity.AppLimit
import com.screengarden.app.domain.limits.AppLimitRule
import com.screengarden.app.domain.limits.AppLimitStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Pausing a limit keeps its chosen duration; all writes validate at the storage boundary. */
class AppLimitRepositoryImpl(
    private val dao: AppLimitDao,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : AppLimitRepository,
    AppLimitStore {
    override fun observeAll(): Flow<List<AppLimit>> = dao.observeAll()
    override fun observeEnabled(): Flow<List<AppLimit>> = dao.observeEnabled()
    override suspend fun get(packageName: String): AppLimit? = dao.get(packageName)
    override suspend fun save(limit: AppLimit) {
        require(limit.packageName.isNotBlank() && limit.dailyLimitMinutes in 1..1440)
        dao.upsert(limit)
    }
    override suspend fun delete(packageName: String) {
        dao.delete(packageName)
    }
    override fun observeLimits(): Flow<List<AppLimitRule>> = dao.observeAll().map { rows ->
        rows.map { AppLimitRule(it.packageName, it.dailyLimitMinutes, it.isEnabled) }
    }
    override suspend fun saveLimit(rule: AppLimitRule) {
        save(AppLimit(rule.packageName, rule.dailyLimitMinutes, rule.isEnabled, nowMillis()))
    }
}
