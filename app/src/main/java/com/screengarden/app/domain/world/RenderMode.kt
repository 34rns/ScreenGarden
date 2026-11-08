// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.world

/** Limits are maxima, not a promise of battery use or a request for background work. */
data class RenderPolicy(val maxFps: Int, val maxPlants: Int, val maxParticles: Int)

enum class RenderMode(val policy: RenderPolicy) {
    INTERACTIVE(RenderPolicy(30, 96, 24)),
    FOCUS(RenderPolicy(15, 64, 8)),
    DESK(RenderPolicy(12, 32, 4)),
    WIDGET(RenderPolicy(0, 1, 0)),
    WALLPAPER(RenderPolicy(0, 48, 12)),
    SNAPSHOT(RenderPolicy(0, 96, 12));

    fun frameRate(visible: Boolean, reducedMotion: Boolean, lowPower: Boolean): Int =
        if (!visible || reducedMotion || lowPower) 0 else policy.maxFps
}
