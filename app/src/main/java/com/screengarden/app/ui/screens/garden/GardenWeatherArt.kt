// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.screengarden.app.domain.garden.GardenWeather
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Twelve deterministic particles maximum, drawn by the existing scene clock without a simulation. */
internal fun DrawScope.drawGardenWeather(
    weather: GardenWeather,
    phase: Float,
    ink: Color,
    lowPower: Boolean,
    maxParticles: Int = 12
) {
    if (weather == GardenWeather.SUN_RAYS) {
        val sun = Offset(size.width * 0.84f, size.height * 0.12f)
        drawCircle(ink.copy(alpha = 0.18f), 14.dp.toPx(), sun)
        if (!lowPower) {
            repeat(8) { index ->
                val angle = index * PI.toFloat() / 4f
                val direction = Offset(cos(angle), sin(angle))
                drawLine(
                    ink.copy(alpha = 0.14f),
                    sun + direction * 20.dp.toPx(),
                    sun + direction * 26.dp.toPx(),
                    2.dp.toPx()
                )
            }
        }
        return
    }
    if (lowPower) return
    repeat(maxParticles.coerceIn(0, 12)) { index ->
        val x = size.width * ((index * 0.137f + phase * 0.04f) % 1f)
        val y = size.height * ((index * 0.083f + phase) % 1f) * 0.48f
        val center = Offset(x, y)
        when (weather) {
            GardenWeather.RAIN -> drawLine(
                ink.copy(alpha = 0.2f),
                center,
                center + Offset(-2.dp.toPx(), 7.dp.toPx()),
                1.dp.toPx()
            )
            GardenWeather.SNOW -> drawCircle(ink.copy(alpha = 0.45f), 2.dp.toPx(), center)
            GardenWeather.FIREFLIES -> drawCircle(
                ink.copy(alpha = 0.15f + (sin(phase * 2 * PI + index).toFloat() + 1f) * 0.15f),
                1.5.dp.toPx(),
                center
            )
            else -> Unit
        }
    }
}
