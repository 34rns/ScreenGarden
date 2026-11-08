// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

/** Still scenes use no animation loop; battery/visibility gates stop every recurring refresh. */
object WallpaperPolicy {
    const val REFRESH_MS: Long = 60_000
    const val LOW_POWER_REFRESH_MS: Long = 300_000
    fun canRefresh(visible: Boolean, screenOn: Boolean, batteryPercent: Int, powerSaver: Boolean): Boolean =
        visible && screenOn && batteryPercent > 15 && !powerSaver
}
