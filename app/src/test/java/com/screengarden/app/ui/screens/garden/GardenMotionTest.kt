// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import android.view.View
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.ui.theme.ScreenGardenTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Proves the scene timer stops when its destination loses visibility. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GardenMotionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sceneClockMovesOnlyWhileEnabled() {
        compose.mainClock.autoAdvance = false
        val enabled = mutableStateOf(true)
        var phase: State<Float>? = null
        compose.setContent { phase = rememberGardenPhase(enabled.value) }
        compose.mainClock.advanceTimeBy(200)
        val first = requireNotNull(phase).value
        compose.mainClock.advanceTimeBy(600)
        assertTrue(requireNotNull(phase).value > first)
        compose.runOnIdle { enabled.value = false }
        compose.mainClock.advanceTimeBy(32)
        assertEquals(0f, requireNotNull(phase).value)
        compose.mainClock.advanceTimeBy(2000)
        assertEquals(0f, requireNotNull(phase).value)
    }

    @Test
    fun hiddenDestinationDisablesMotion() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry.createUnsafe(this)
            override val lifecycle: Lifecycle = registry
        }
        owner.registry.currentState = Lifecycle.State.STARTED
        var enabled = true
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                val motion = rememberGardenMotionEnabled()
                SideEffect { enabled = motion }
            }
        }
        compose.runOnIdle { assertFalse(enabled) }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(enabled) }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        compose.waitForIdle()
        compose.runOnIdle { assertFalse(enabled) }
    }

    @Test
    fun reducedMotionReadsCurrentSettingRatherThanCachedAnimatorScale() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        val previous = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry.createUnsafe(this)
            override val lifecycle: Lifecycle = registry
        }
        owner.registry.currentState = Lifecycle.State.RESUMED
        var enabled = true
        val cachedScale = android.animation.ValueAnimator::class.java.getDeclaredField("sDurationScale")
            .apply { isAccessible = true }
        val previousCache = cachedScale.getFloat(null)
        try {
            // The animator's process cache can still report enabled when this setting is already zero.
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            cachedScale.setFloat(null, 1f)
            compose.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    val motion = rememberGardenMotionEnabled()
                    SideEffect { enabled = motion }
                }
            }
            compose.runOnIdle { assertFalse(enabled) }
        } finally {
            cachedScale.setFloat(null, previousCache)
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, previous)
        }
    }

    @Test
    fun growthBloomAndWiltPassThroughIntermediateDrawingValues() {
        compose.mainClock.autoAdvance = false
        val plant = mutableStateOf(Plant("motion-test", "flower", growthPoints = 10, health = 100))
        var view: View? = null
        var visuals: PlantVisuals? = null
        compose.setContent {
            view = LocalView.current
            ScreenGardenTheme {
                CompositionLocalProvider(
                    LocalPlantRenderer provides PlantRenderer { _, _, _, value ->
                        visuals = value
                    }
                ) {
                    GardenPlantArt(plant.value, androidx.compose.ui.Modifier.size(120.dp), motionEnabled = true)
                }
            }
        }
        fun draw(): PlantVisuals {
            compose.runOnIdle {
                val root = requireNotNull(view)
                val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                root.draw(Canvas(bitmap))
                bitmap.recycle()
            }
            return requireNotNull(visuals)
        }
        compose.mainClock.advanceTimeBy(32)
        assertEquals(0.5f, draw().growth)
        compose.runOnIdle {
            plant.value = plant.value.copy(growthPoints = 45, health = 10, condition = PlantCondition.WILTING)
        }
        // Let recomposition publish the target before advancing the animation clock.
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(450)
        val middle = draw()
        assertTrue("Intermediate growth: $middle", middle.growth > 0.5f && middle.growth < 1f)
        assertTrue("Intermediate bloom: $middle", middle.bloom > 0f && middle.bloom < 1f)
        assertTrue("Intermediate wilt: $middle", middle.wilt > 0f && middle.wilt < 1f)
        compose.mainClock.advanceTimeBy(1000)
        val end = draw()
        assertEquals(1f, end.growth)
        assertEquals(1f, end.bloom)
        assertEquals(1f, end.wilt)
        compose.runOnIdle { plant.value = plant.value.copy(health = 100, condition = PlantCondition.GROWING) }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1200)
        assertEquals(0f, draw().wilt)
    }
}
