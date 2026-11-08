// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.screengarden.app.domain.world.RenderMode

/** Dialog-scoped window overrides: no system setting or permission, restored on pause/exit. */
@Composable
internal fun WorldWindow(mode: RenderMode) {
    val view = LocalView.current
    val owner = LocalLifecycleOwner.current
    DisposableEffect(view, owner, mode) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window == null) return@DisposableEffect onDispose { }
        val previousAttributes = WindowManager.LayoutParams().apply { copyFrom(window.attributes) }
        val brightness = window.attributes.screenBrightness
        val keepOn = window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
        val controller = WindowCompat.getInsetsController(window, view)
        val barBehavior = controller.systemBarsBehavior
        fun restore() {
            window.attributes = window.attributes.apply {
                screenBrightness = brightness
                if (Build.VERSION.SDK_INT >= 28) {
                    layoutInDisplayCutoutMode = previousAttributes.layoutInDisplayCutoutMode
                }
            }
            if (!keepOn) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.systemBarsBehavior = barBehavior
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        fun apply() {
            if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                restore()
                return
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.attributes = window.attributes.apply {
                if (mode == RenderMode.DESK) screenBrightness = 0.06f
                if (Build.VERSION.SDK_INT >= 28) {
                    layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= 30) {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    } else {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
            }
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        val observer = LifecycleEventObserver { _, _ -> apply() }
        owner.lifecycle.addObserver(observer)
        apply()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            restore()
        }
    }
}

/** Visible Settings only: sticky battery status needs no permission and never starts Desk by itself. */
@Composable
internal fun rememberWorldCharging(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    var charging by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                val status = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                charging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == android.os.BatteryManager.BATTERY_STATUS_FULL
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return charging
}
