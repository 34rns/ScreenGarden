// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.invalidationTrackerFlow
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.domain.settings.SettingsRepository
import com.screengarden.app.ui.widgets.GardenWidget
import com.screengarden.app.ui.widgets.GardenWidgetReceiver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/** Observes only installed widgets; coalesces Room checkpoints and preference edits. */
@OptIn(FlowPreview::class)
class GardenWidgetUpdates(
    private val context: Context,
    private val database: () -> ScreenGardenDatabase,
    private val settings: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observer: Job? = null

    fun start() {
        if (observer?.isActive == true) return
        val manager = AppWidgetManager.getInstance(context)
        if (manager.getAppWidgetIds(ComponentName(context, GardenWidgetReceiver::class.java)).isEmpty()) return
        observer = scope.launch {
            try {
                val goals = combine(settings.dailyGoalMs, settings.pickupGoal, settings.streakThreshold) { a, b, c ->
                    Triple(a, b, c)
                }
                combine(
                    database().invalidationTrackerFlow("daily_stats", "plants", "app_limits"),
                    goals
                ) { _, _ -> Unit }.debounce(1_000).collect {
                    try {
                        GardenWidget().updateAll(context)
                    } catch (failure: Exception) {
                        if (failure is CancellationException) throw failure
                        // The host's scheduled update retries; widget trouble must not crash the app.
                    }
                }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                // Locked storage is retried on the next process start or host update.
            }
        }
    }

    fun stop() {
        observer?.cancel()
        observer = null
    }
}
