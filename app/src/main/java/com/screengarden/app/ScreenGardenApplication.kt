// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app

import android.app.Application
import com.screengarden.app.di.AppContainer

/**
 * Application entry point. Owns the single [AppContainer] instance that manual DI
 * hands to screens and view models.
 */
class ScreenGardenApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
