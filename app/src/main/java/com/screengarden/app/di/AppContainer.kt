// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.di

import android.content.Context
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.data.settings.DataStoreSettingsRepository
import com.screengarden.app.data.settings.SettingsRepository
import com.screengarden.app.data.settings.settingsDataStore

/**
 * Manual dependency injection root, created once by [com.screengarden.app.ScreenGardenApplication].
 *
 * DECISION: manual DI instead of Hilt.
 * - No kapt step (only KSP for Room, which we need anyway), so the toolchain
 *   stays simple.
 * - The app has one Activity and no dynamic feature modules, so a graph library
 *   buys nothing yet.
 * - Everything is constructed here and nowhere else, so a screen cannot quietly
 *   build its own database or DataStore.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /**
     * The Room database. Lazy so a cold start does not touch disk until a screen
     * actually asks for data.
     */
    val database: ScreenGardenDatabase by lazy { ScreenGardenDatabase.build(appContext) }

    /**
     * User settings (AGENT_MEMORY 16.7), read at the composition root to pick the
     * theme and later by the settings UI.
     */
    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(appContext.settingsDataStore)
    }
}
