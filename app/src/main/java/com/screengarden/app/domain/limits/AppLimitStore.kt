// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.limits

import kotlinx.coroutines.flow.Flow

/** Launchable app metadata stays local; package name remains stable when its label changes. */
data class LaunchableApp(val packageName: String, val label: String)

data class AppLimitRule(val packageName: String, val dailyLimitMinutes: Int, val isEnabled: Boolean = true)

/** Local storage boundary used by the limit editor without Android or Room types. */
interface AppLimitStore {
    fun observeLimits(): Flow<List<AppLimitRule>>
    suspend fun saveLimit(rule: AppLimitRule)
}
