// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.data.local.entity.AppLimit
import kotlinx.coroutines.flow.Flow

/**
 * User-configured per-app daily limits.
 *
 * INTERFACE ONLY for now (Task 1.1); Phase 3.2 implements it.
 */
interface AppLimitRepository {

    fun observeAll(): Flow<List<AppLimit>>

    fun observeEnabled(): Flow<List<AppLimit>>

    suspend fun get(packageName: String): AppLimit?

    suspend fun save(limit: AppLimit)

    suspend fun delete(packageName: String)
}
