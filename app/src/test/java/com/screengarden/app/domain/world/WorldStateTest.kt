// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.world

import com.screengarden.app.domain.garden.Plant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldStateTest {
    private val bounds = WorldBounds(-4f, -3f, 4f, 3f)

    @Test fun cameraCannotPanOutsideTheField() {
        val camera = WorldCamera().pan(100_000f, -100_000f, bounds)
        assertEquals(4f, camera.x)
        assertEquals(-3f, camera.y)
    }

    @Test fun invalidGestureDoesNotLoseTheCamera() {
        val camera = WorldCamera(1f, 1f)
        assertEquals(camera, camera.pan(Float.NaN, 0f, bounds))
        assertEquals(camera, camera.zoomBy(Float.POSITIVE_INFINITY, 0f, 0f, bounds))
        assertEquals(camera, camera.zoomBy(-1f, 0f, 0f, bounds))
    }

    @Test fun zoomIsBoundedAndKeepsItsWorldAnchor() {
        val camera = WorldCamera().zoomBy(2f, 2f, 0f, bounds)
        assertEquals(1f, camera.x)
        assertEquals(2f, (2f - camera.x) * camera.zoom)
        assertEquals(WorldCamera.MAX_ZOOM, camera.zoomBy(100_000f, 0f, 0f, bounds).zoom)
        assertEquals(WorldCamera.MIN_ZOOM, camera.zoomBy(0.00001f, 0f, 0f, bounds).zoom)
    }

    @Test fun mappingIgnoresQueryOrderAndRetainsOlderPlants() {
        val old = Plant("old", "fern", 45, plantedAtUtcMillis = 100)
        val newer = Plant("new", "flower", 10, plantedAtUtcMillis = 200)
        assertEquals(mapWorldPlants(listOf(old, newer)), mapWorldPlants(listOf(newer, old)))
        assertEquals(mapWorldPlants(listOf(old)).first(), mapWorldPlants(listOf(old, newer)).first())
    }

    @Test fun positionsRemainUniqueInALargeWorld() {
        val plants = mapWorldPlants((0..999).map { Plant("plant-$it", "fern", plantedAtUtcMillis = it.toLong()) })
        assertEquals(1000, plants.size)
        assertEquals(1000, plants.map { it.x to it.y }.toSet().size)
        assertTrue(plants.all { it.x.isFinite() && it.y.isFinite() })
    }

    @Test fun staticModesAndLowPowerNeverStartAFrameLoop() {
        assertEquals(0, RenderMode.WIDGET.policy.maxFps)
        assertEquals(0, RenderMode.SNAPSHOT.policy.maxFps)
        assertEquals(0, RenderMode.WALLPAPER.policy.maxFps)
        assertEquals(12, RenderMode.DESK.policy.maxFps)
        assertEquals(0, RenderMode.FOCUS.frameRate(visible = false, reducedMotion = false, lowPower = false))
        assertEquals(0, RenderMode.DESK.frameRate(visible = true, reducedMotion = true, lowPower = false))
        assertEquals(0, RenderMode.INTERACTIVE.frameRate(visible = true, reducedMotion = false, lowPower = true))
    }
}
