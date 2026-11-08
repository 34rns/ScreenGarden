// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.debug

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.screengarden.app.R

/** Fake-only visual fixture; no timer, storage or usage is read. Absent from release. */
class DemoFocusTileService : TileService() {
    private var running = false
    override fun onStartListening() {
        super.onStartListening()
        render()
    }
    override fun onClick() {
        super.onClick()
        running = !running
        render()
    }
    private fun render() {
        qsTile?.apply {
            state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.tile_focus)
            val action = getString(if (running) R.string.tile_stop else R.string.tile_start)
            contentDescription = action
            if (Build.VERSION.SDK_INT >= 29) subtitle = action
            updateTile()
        }
    }
}
