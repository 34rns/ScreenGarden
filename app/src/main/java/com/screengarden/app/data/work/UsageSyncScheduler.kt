// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.work

import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Decides *when* usage is synced; [com.screengarden.app.data.sync.DailyUsageSync]
 * decides *what* is written (P06).
 *
 * Two triggers, because a periodic worker alone is not enough in either direction:
 * - periodic: at least every [PERIOD_SYNC_HOURS], so a day is complete even when the
 *   app is never opened (WorkManager batches it into a maintenance window);
 * - on open: immediately, because "yesterday's number is missing" is exactly what a
 *   user notices when they open Insights, and waiting hours for a periodic run would
 *   look like a bug.
 *
 * Both use unique work names, so repeated calls cannot pile up duplicate jobs:
 * `UPDATE` keeps the existing periodic registration (and picks up a changed period
 * or constraint on upgrade) while `KEEP` leaves a one-shot sync that is already
 * queued or running alone instead of cancelling and restarting it.
 *
 * WorkManager is taken as a provider and resolved lazily, because
 * `WorkManager.getInstance()` throws when the library is not initialised: that
 * happens on a direct-boot device before the user unlocks it, in a build with the
 * initializer removed, and in JVM tests that only stand up an Application. Syncing is
 * then simply skipped for that process instead of crashing the launch - a missing
 * background sync is recoverable on the next start, a crash at startup is not.
 */
class UsageSyncScheduler(private val workManagerProvider: () -> WorkManager) {

    /** Registers the repeating sync; idempotent, so call it on every app start. */
    fun schedulePeriodicSync() {
        val workManager = workManagerOrNull() ?: return
        val request = PeriodicWorkRequestBuilder<UsageSyncWorker>(PERIOD_SYNC_HOURS, TimeUnit.HOURS)
            .setConstraints(batteryNotLow())
            .build()
        workManager.enqueueUniquePeriodicWork(
            UsageSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * Asks for a sync right now.
     *
     * No constraint here: the user has the app open, so the device is awake and on
     * battery only in the sense that it is in their hand. The periodic run keeps its
     * battery constraint for the case where nobody is looking.
     */
    fun syncNow() {
        val workManager = workManagerOrNull() ?: return
        val request = OneTimeWorkRequestBuilder<UsageSyncWorker>().build()
        workManager.enqueueUniqueWork(
            UsageSyncWorker.ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Reading usage stats is cheap, but a sync during a system-initiated low-battery
     * shutdown is pointless: nothing will be looked at until the next charge.
     */
    private fun batteryNotLow(): Constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()

    /** The WorkManager instance, or null when the library is not available. */
    private fun workManagerOrNull(): WorkManager? = try {
        workManagerProvider()
    } catch (unavailable: IllegalStateException) {
        Log.w(TAG, "WorkManager unavailable, skipping the usage sync for this process", unavailable)
        null
    }

    private companion object {
        const val TAG = "UsageSyncScheduler"

        /**
         * Six hours, not twenty-four: only *today's* row changes between midnight
         * passes, and a day-long period would leave Insights showing nothing for
         * today until the following night.
         */
        const val PERIOD_SYNC_HOURS = 6L
    }
}
