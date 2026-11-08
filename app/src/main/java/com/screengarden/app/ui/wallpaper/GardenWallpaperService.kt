// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.os.BatteryManager
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.ContextCompat
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.debug.DebugCatalog
import com.screengarden.app.domain.garden.GardenEnvironment
import com.screengarden.app.domain.garden.WallpaperPolicy
import com.screengarden.app.domain.garden.summarizeGardenStatus
import com.screengarden.app.ui.screens.garden.GardenUiState
import com.screengarden.app.ui.screens.garden.PackPlantRenderer
import com.screengarden.app.ui.screens.garden.renderGardenSnapshot
import com.screengarden.app.ui.theme.screenGardenColors
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Still, locally refreshed garden. No usage collection, rewards, animation loop or background service start. */
class GardenWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = GardenWallpaperEngine()

    inner class GardenWallpaperEngine : Engine() {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val power get() = getSystemService(PowerManager::class.java)
        private var visible = false
        private var ready = false
        private var battery = 100
        private var frame: Bitmap? = null
        private var refresh: Job? = null
        private var refreshAllowed = false
        private var pixelWidth = 720
        private var pixelHeight = 1600
        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                    battery = (level * 100 / scale).coerceIn(0, 100)
                }
                val allowed = WallpaperPolicy.canRefresh(visible, power.isInteractive, battery, power.isPowerSaveMode)
                if (allowed != refreshAllowed) restart()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            ContextCompat.registerReceiver(
                this@GardenWallpaperService,
                receiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            restart()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            pixelWidth = width.coerceIn(96, 720)
            pixelHeight = (height.toLong() * pixelWidth / width.coerceAtLeast(1)).toInt().coerceIn(96, 2048)
            ready = true
            restart()
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) = drawFrame()

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            ready = false
            refresh?.cancel()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            scope.cancel()
            unregisterReceiver(receiver)
            frame?.recycle()
            frame = null
            super.onDestroy()
        }

        private fun restart() {
            refresh?.cancel()
            refreshAllowed = WallpaperPolicy.canRefresh(visible, power.isInteractive, battery, power.isPowerSaveMode)
            if (!ready || !visible || !power.isInteractive) return
            drawFrame()
            val recurring = refreshAllowed
            // A cold low-battery preview gets one still frame, then no recurring work.
            if (!recurring && frame != null) return
            refresh = scope.launch {
                do {
                    var lowPower = false
                    try {
                        val image = withContext(Dispatchers.IO) {
                            val container = (application as ScreenGardenApplication).container
                            val settings = container.settingsRepository
                            val now = ZonedDateTime.now()
                            val today = LocalDay.startOfDay(now.toInstant().toEpochMilli(), now.zone)
                            val fake = if (isPreview) DebugCatalog.wallpaperPreviewState() else null
                            val state = fake ?: run {
                                container.backupRepository.recoverPendingSettings()
                                val snapshot = container.gardenRepository.observeSnapshot(today, today).first()
                                val status = summarizeGardenStatus(
                                    snapshot,
                                    today,
                                    (settings.dailyGoalMs.first() / 60_000).toInt(),
                                    settings.pickupGoal.first(),
                                    settings.streakThreshold.first(),
                                    container.usageRepository.isUsageAccessGranted(),
                                    now.zone
                                )
                                GardenUiState(
                                    loading = false,
                                    plants = snapshot.plants,
                                    environment = GardenEnvironment.at(now, settings.hemisphere.first(), status.score),
                                    lowPowerVisuals = settings.lowPowerVisuals.first()
                                )
                            }
                            lowPower = state.lowPowerVisuals
                            val colors = screenGardenColors(
                                settings.theme.first(),
                                settings.accentColor.first(),
                                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                                    Configuration.UI_MODE_NIGHT_YES
                            )
                            val packs = container.plantPacks().packs
                            renderGardenSnapshot(
                                state,
                                colors,
                                renderer = PackPlantRenderer(
                                    packs.associate {
                                        it.metadata.id to
                                            it.images
                                    }
                                ),
                                width = pixelWidth,
                                height = pixelHeight,
                                framed = false
                            ).asAndroidBitmap()
                        }
                        val old = frame
                        frame = image
                        drawFrame()
                        old?.recycle()
                    } catch (failure: Exception) {
                        if (failure is CancellationException) throw failure
                        // Keep the last frame. A later visible refresh can recover without deleting user data.
                    }
                    if (!recurring) break
                    delay(if (lowPower) WallpaperPolicy.LOW_POWER_REFRESH_MS else WallpaperPolicy.REFRESH_MS)
                } while (WallpaperPolicy.canRefresh(visible, power.isInteractive, battery, power.isPowerSaveMode))
            }
        }

        private fun drawFrame() {
            if (!ready || !visible || !power.isInteractive) return
            val bitmap = frame ?: return
            val canvas = try {
                surfaceHolder.lockCanvas()
            } catch (_: RuntimeException) {
                null
            } ?: return
            try {
                canvas.drawBitmap(
                    bitmap,
                    null,
                    Rect(0, 0, canvas.width, canvas.height),
                    Paint(Paint.FILTER_BITMAP_FLAG)
                )
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
