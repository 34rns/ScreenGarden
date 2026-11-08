// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.work

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Android's 15-minute minimum is approximate; Doze and battery constraints can delay milestones. */
class NudgeScheduler(private val workManagerProvider: () -> WorkManager) {
    fun schedule() {
        val manager = managerOrNull() ?: return
        manager.enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<NudgeWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build()).build()
        )
    }

    fun checkNow() {
        managerOrNull()?.enqueueUniqueWork(
            "gentle_nudges_now",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<NudgeWorker>().build()
        )
    }

    private fun managerOrNull(): WorkManager? = try {
        workManagerProvider()
    } catch (_: IllegalStateException) {
        null
    }

    companion object {
        const val PERIODIC_NAME = "gentle_nudges_periodic"
    }
}
