// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.screengarden.app.ui.wallpaper.GardenWallpaperService

/** User-initiated system preview; never applies a wallpaper automatically. */
fun openWallpaperPreview(context: Context): Boolean = try {
    context.startActivity(
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(context, GardenWallpaperService::class.java)
        )
    )
    true
} catch (_: RuntimeException) {
    false
}
