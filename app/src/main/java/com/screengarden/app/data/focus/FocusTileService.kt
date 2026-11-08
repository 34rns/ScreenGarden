// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.focus

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.screengarden.app.R
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.domain.focus.FocusTag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Only user taps start/stop focus. No timer or usage polling belongs in the tile. */
class FocusTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val command = Mutex()
    private var listening: Job? = null
    private val container get() = (application as ScreenGardenApplication).container

    override fun onStartListening() {
        super.onStartListening()
        listening?.cancel()
        listening = scope.launch {
            try {
                container.focusSessionRepository.observeRunning().collect { sessions -> render(sessions.isNotEmpty()) }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                qsTile?.apply {
                    state = Tile.STATE_UNAVAILABLE
                    updateTile()
                }
            }
        }
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        unlockAndRun {
            scope.launch {
                command.withLock {
                    try {
                        val current = container.focusSessionRepository.activeSession()
                        if (current == null) {
                            container.focusController.start(25, FocusTag.STUDY)
                        } else {
                            container.focusController.stop(current.id)
                        }
                    } catch (failure: Exception) {
                        if (failure is CancellationException) throw failure
                        qsTile?.apply {
                            state = Tile.STATE_UNAVAILABLE
                            updateTile()
                        }
                    }
                }
            }
        }
    }

    private fun render(running: Boolean) {
        qsTile?.apply {
            state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.tile_focus)
            val action = getString(if (running) R.string.tile_stop else R.string.tile_start)
            contentDescription = action
            if (Build.VERSION.SDK_INT >= 29) subtitle = action
            updateTile()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
