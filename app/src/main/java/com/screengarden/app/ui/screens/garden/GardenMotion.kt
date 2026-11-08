// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** A single scene clock, removed from composition when hidden or motion is disabled. */
@Composable
internal fun rememberGardenPhase(enabled: Boolean): State<Float> {
    if (!enabled) return rememberUpdatedState(0f)
    val transition = rememberInfiniteTransition(label = "garden breeze")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "garden phase"
    )
}

/** Observe the platform's animation switch, battery saver and destination visibility. */
@Composable
internal fun rememberGardenMotionEnabled(): Boolean {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var enabled by remember(context, owner) { mutableStateOf(false) }
    DisposableEffect(context, owner) {
        fun update() {
            enabled = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                // Read the setting itself: the animator process cache can lag this observer callback.
                Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f &&
                context.getSystemService(PowerManager::class.java)?.isPowerSaveMode != true
        }
        val lifecycleObserver = LifecycleEventObserver { _, _ -> update() }
        val scaleObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                update()
            }
        }
        val powerObserver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                update()
            }
        }
        owner.lifecycle.addObserver(lifecycleObserver)
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            scaleObserver
        )
        ContextCompat.registerReceiver(
            context,
            powerObserver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        update()
        onDispose {
            owner.lifecycle.removeObserver(lifecycleObserver)
            context.contentResolver.unregisterContentObserver(scaleObserver)
            context.unregisterReceiver(powerObserver)
        }
    }
    return enabled
}
