// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.domain.focus.FocusSessionMachine
import com.screengarden.app.domain.focus.FocusStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Reboot cancels a checkpoint; it never starts a foreground service in the background. */
class FocusBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as ScreenGardenApplication).container
                val repository = container.focusSessionRepository
                val session = repository.activeSession() ?: return@launch
                repository.checkpoint(
                    FocusSessionMachine.end(
                        session,
                        FocusStatus.CANCELLED,
                        session.actualDurationMs,
                        System.currentTimeMillis()
                    )
                )
            } finally {
                pending.finish()
            }
        }
    }
}
