// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.screengarden.app.domain.garden.GardenEnvironment
import com.screengarden.app.domain.garden.GardenLight
import com.screengarden.app.domain.garden.Season

private val SunsetLight = Color(0xFFF4D4B6)
private val SunsetDark = Color(0xFF574332)
private val AutumnLight = Color(0xFFE2D6B2)
private val AutumnDark = Color(0xFF4C4936)

internal data class GardenEnvironmentColors(val sky: Color, val horizon: Color, val ground: Color)

/** Scene tokens preserve each theme's readable plant-label foreground/ground pair. */
internal fun gardenEnvironmentColors(environment: GardenEnvironment, colors: ColorScheme): GardenEnvironmentColors {
    val dark = colors.surface.red + colors.surface.green + colors.surface.blue < 1.5f
    val sky = when (environment.light) {
        GardenLight.DAY -> colors.tertiaryContainer
        GardenLight.SUNSET -> if (dark) SunsetDark else SunsetLight
        GardenLight.NIGHT -> GardenNightColors.tertiaryContainer
    }
    val ground = when (environment.season) {
        Season.WINTER -> lerp(colors.secondaryContainer, colors.surface, 0.45f)
        Season.AUTUMN -> lerp(colors.secondaryContainer, if (dark) AutumnDark else AutumnLight, 0.35f)
        Season.SUMMER -> lerp(colors.secondaryContainer, colors.primaryContainer, 0.3f)
        Season.SPRING -> colors.secondaryContainer
    }
    return GardenEnvironmentColors(sky, lerp(sky, ground, 0.35f), ground)
}
