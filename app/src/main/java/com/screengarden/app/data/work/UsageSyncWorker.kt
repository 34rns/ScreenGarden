// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.data.sync.DailyUsageSyncResult
import kotlinx.coroutines.CancellationException

/**
 * Runs [com.screengarden.app.data.sync.DailyUsageSync] in the background (P06).
 *
 * The worker itself contains no logic: it resolves the container, runs the sync and
 * translates the outcome into a WorkManager result. Everything worth testing lives
 * in the sync class, which needs no Android at all.
 *
 * Result mapping:
 * - [DailyUsageSyncResult.Synced] -> success. Running again later is the scheduler's
 *   job, not a retry of this run.
 * - [DailyUsageSyncResult.MissingUsageAccess] -> success as well. Retrying cannot
 *   grant a permission only the user can give, and `Result.failure()` would make the
 *   periodic work look broken; the app simply has no data until P07 offers the
 *   settings screen (D46).
 * - an exception (a dead database, a revoked provider) -> retry, with WorkManager's
 *   own backoff, because that is a transient condition it can recover from.
 */
class UsageSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as ScreenGardenApplication).container
        return try {
            container.backupRepository.recoverPendingSettings()
            when (val result = container.dailyUsageSync.run()) {
                is DailyUsageSyncResult.Synced -> {
                    // Logging, not telemetry (H3): visible in `adb logcat`, never sent.
                    Log.i(TAG, "Synced usage for ${result.days.size} day(s)")
                    Result.success()
                }
                DailyUsageSyncResult.MissingUsageAccess -> {
                    Log.i(TAG, "Skipped usage sync: Usage access is not granted")
                    Result.success()
                }
            }
        } catch (cancellation: CancellationException) {
            // The system cancelled us (constraints lost, unique work replaced).
            // Swallowing this would leave a coroutine that thinks it is still alive.
            throw cancellation
        } catch (error: Exception) {
            Log.w(TAG, "Usage sync failed, will retry", error)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UsageSyncWorker"

        /** Unique names, so the app never queues two copies of the same sync. */
        const val PERIODIC_WORK_NAME = "usage_daily_sync_periodic"

        /** One-shot work enqueued when the app is opened. */
        const val ONE_TIME_WORK_NAME = "usage_daily_sync_now"
    }
}
