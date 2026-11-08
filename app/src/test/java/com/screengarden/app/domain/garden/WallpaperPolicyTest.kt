// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperPolicyTest {
    @Test fun hiddenScreenOffLowBatteryAndSaverAlwaysPause() {
        assertTrue(WallpaperPolicy.canRefresh(true, true, 16, false))
        assertFalse(WallpaperPolicy.canRefresh(false, true, 100, false))
        assertFalse(WallpaperPolicy.canRefresh(true, false, 100, false))
        assertFalse(WallpaperPolicy.canRefresh(true, true, 15, false))
        assertFalse(WallpaperPolicy.canRefresh(true, true, 100, true))
        assertTrue(WallpaperPolicy.REFRESH_MS >= 60_000)
        assertTrue(WallpaperPolicy.LOW_POWER_REFRESH_MS >= WallpaperPolicy.REFRESH_MS)
    }
}
