// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.world

/** Finite field bounds in world units, independent of screen pixels and Android. */
data class WorldBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init {
        require(listOf(left, top, right, bottom).all { it.isFinite() } && left <= right && top <= bottom)
    }
}

/** Bounded camera; malformed gesture samples preserve the last usable view. */
data class WorldCamera(val x: Float = 0f, val y: Float = 0f, val zoom: Float = 1f) {
    init {
        require(x.isFinite() && y.isFinite() && zoom.isFinite() && zoom in MIN_ZOOM..MAX_ZOOM)
    }

    fun pan(dx: Float, dy: Float, bounds: WorldBounds): WorldCamera {
        if (!dx.isFinite() || !dy.isFinite()) return this
        return copy(
            x = (x.toDouble() + dx).coerceIn(bounds.left.toDouble(), bounds.right.toDouble()).toFloat(),
            y = (y.toDouble() + dy).coerceIn(bounds.top.toDouble(), bounds.bottom.toDouble()).toFloat()
        )
    }

    fun zoomBy(factor: Float, anchorX: Float, anchorY: Float, bounds: WorldBounds): WorldCamera {
        if (!factor.isFinite() || factor <= 0f || !anchorX.isFinite() || !anchorY.isFinite()) return this
        val nextZoom = (zoom.toDouble() * factor).coerceIn(MIN_ZOOM.toDouble(), MAX_ZOOM.toDouble()).toFloat()
        val nextX = anchorX.toDouble() + (x.toDouble() - anchorX) * zoom / nextZoom
        val nextY = anchorY.toDouble() + (y.toDouble() - anchorY) * zoom / nextZoom
        return WorldCamera(
            nextX.coerceIn(bounds.left.toDouble(), bounds.right.toDouble()).toFloat(),
            nextY.coerceIn(bounds.top.toDouble(), bounds.bottom.toDouble()).toFloat(),
            nextZoom
        )
    }

    companion object {
        const val MIN_ZOOM: Float = 0.25f
        const val MAX_ZOOM: Float = 3.5f
    }
}
