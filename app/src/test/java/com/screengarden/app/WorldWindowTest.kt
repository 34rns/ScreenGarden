// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app

import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.screengarden.app.domain.world.RenderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WorldWindowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun deskRestoresBrightnessFlagsAndBarBehaviorOnPause() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry.createUnsafe(this)
            override val lifecycle: Lifecycle = registry
        }
        owner.registry.currentState = Lifecycle.State.STARTED
        var window: Window? = null
        var previousBehavior = 0
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                Dialog(onDismissRequest = { }) {
                    val view = LocalView.current
                    window = (view.parent as DialogWindowProvider).window
                    previousBehavior = WindowCompat.getInsetsController(requireNotNull(window), view).systemBarsBehavior
                    CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                        WorldWindow(RenderMode.DESK)
                    }
                }
            }
        }
        compose.runOnIdle {
            val target = requireNotNull(window)
            assertEquals(-1f, target.attributes.screenBrightness)
            owner.registry.currentState = Lifecycle.State.RESUMED
        }
        compose.waitForIdle()
        compose.runOnIdle {
            val target = requireNotNull(window)
            assertEquals(0.06f, target.attributes.screenBrightness)
            assertEquals(
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
                target.attributes.layoutInDisplayCutoutMode
            )
            assertTrue(target.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
            owner.registry.currentState = Lifecycle.State.STARTED
        }
        compose.waitForIdle()
        compose.runOnIdle {
            val target = requireNotNull(window)
            assertEquals(-1f, target.attributes.screenBrightness)
            assertEquals(0, target.attributes.layoutInDisplayCutoutMode)
            assertFalse(target.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
            assertEquals(
                previousBehavior,
                WindowCompat.getInsetsController(target, target.decorView).systemBarsBehavior
            )
        }
    }
}
