// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.focus

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.screengarden.app.domain.focus.FocusTag

/** Android launch boundary; the activity supplies its callbacks to the focus UI. */
class FocusController(private val context: Context) {
    fun start(minutes: Int, tag: FocusTag, plantId: String? = null) {
        require(minutes in 1..240)
        ContextCompat.startForegroundService(
            context,
            Intent(context, FocusTimerService::class.java).apply {
                action = FocusTimerService.ACTION_START
                putExtra(FocusTimerService.EXTRA_MINUTES, minutes)
                putExtra(FocusTimerService.EXTRA_TAG, tag.name)
                putExtra(FocusTimerService.EXTRA_PLANT, plantId)
            }
        )
    }

    fun stop(sessionId: String) {
        context.startService(
            Intent(context, FocusTimerService::class.java).apply {
                action = FocusTimerService.ACTION_STOP
                putExtra(FocusTimerService.EXTRA_SESSION, sessionId)
            }
        )
    }
}
