// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app

import android.app.Application
import com.screengarden.app.di.AppContainer

/**
 * Application entry point. Owns the single [AppContainer] instance that manual DI
 * hands to screens and view models.
 *
 * Also registers the repeating usage sync (P06). This is the right place and not the
 * Activity: the registration has to survive a launch that never draws UI, and
 * `enqueueUniquePeriodicWork(UPDATE)` is idempotent, so running it on every process
 * start cannot create a second schedule.
 */
class ScreenGardenApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Scheduling only; the first sync itself happens when the app is opened or
        // when WorkManager decides to run the periodic work (UsageSyncScheduler).
        container.usageSyncScheduler.schedulePeriodicSync()
        container.nudgeScheduler.schedule()
        container.widgetUpdates.start()
    }
}
