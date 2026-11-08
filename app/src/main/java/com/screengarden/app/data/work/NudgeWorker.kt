// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.screengarden.app.ScreenGardenApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** No foreground service or exact alarm; settings/permission/quiet-hour checks run before usage reads. */
class NudgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        withContext(Dispatchers.IO) {
            val container = (applicationContext as ScreenGardenApplication).container
            container.backupRepository.recoverPendingSettings()
            container.nudgeCheck.run()
        }
        Result.success()
    } catch (failure: Exception) {
        if (failure is CancellationException) throw failure
        Result.retry()
    }
}
