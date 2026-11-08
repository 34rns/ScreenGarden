// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.world

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.dp
import com.screengarden.app.domain.garden.GardenLight
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.domain.garden.PlantStage
import com.screengarden.app.domain.world.RenderMode
import com.screengarden.app.domain.world.WorldPlant
import com.screengarden.app.domain.world.WorldSpace
import com.screengarden.app.domain.world.WorldState
import com.screengarden.app.ui.screens.garden.PlantPalette
import com.screengarden.app.ui.screens.garden.PlantRenderer
import com.screengarden.app.ui.screens.garden.PlantVisuals
import com.screengarden.app.ui.screens.garden.drawGardenWeather
import com.screengarden.app.ui.theme.GardenEnvironmentColors
import com.screengarden.app.ui.theme.gardenEnvironmentColors
import kotlin.math.PI
import kotlin.math.sin

/** All scene composition is here. Platform adapters supply a canvas and explicit state. */
internal fun DrawScope.drawWorld(
    state: WorldState,
    mode: RenderMode,
    colors: ColorScheme,
    renderer: PlantRenderer,
    plantPalette: PlantPalette? = null
) {
    val sceneColors = gardenEnvironmentColors(state.environment, colors)
    if (state.backdrop) {
        drawWorldBackdrop(sceneColors, colors, state.environment.light)
        if (mode.policy.maxParticles > 0) {
            val motes = if (state.streakDays > 0 && state.environment.light != GardenLight.NIGHT) 6 else 0
            drawGardenWeather(
                state.environment.weather,
                state.phase,
                colors.onTertiaryContainer,
                state.lowPowerVisuals,
                (mode.policy.maxParticles - motes).coerceIn(0, 12)
            )
        }
        if (state.streakDays > 0 && !state.lowPowerVisuals && state.environment.light != GardenLight.NIGHT) {
            repeat(6.coerceAtMost(mode.policy.maxParticles)) { index ->
                val x = size.width * ((index * 0.17f + state.phase * 0.08f) % 1f)
                val y = size.height * (0.15f + index * 0.07f) +
                    sin((state.phase * 2 * PI + index).toFloat()) * 4.dp.toPx()
                drawCircle(colors.onTertiaryContainer.copy(alpha = 0.12f), 1.5.dp.toPx(), Offset(x, y))
            }
            if (state.streakDays >= 3) {
                val center = Offset(size.width * (0.18f + state.phase * 0.6f), size.height * 0.16f)
                val wing = 2.dp.toPx() + kotlin.math.abs(sin(state.phase * 12 * PI).toFloat()) * 3.dp.toPx()
                drawOval(colors.tertiary, center - Offset(wing, 3.dp.toPx()), Size(wing, 6.dp.toPx()))
                drawOval(colors.tertiary, center, Size(wing, 6.dp.toPx()))
            }
        }
    }
    val palette = plantPalette ?: PlantPalette(
        colors.primary,
        colors.secondary,
        colors.secondary,
        colors.tertiary,
        colors.onTertiary,
        colors.onSecondaryContainer
    )
    state.plants.take(mode.policy.maxPlants).forEach { placed ->
        val rect = worldPlantRect(placed, state, size.width, size.height)
        if (rect[2] <= 0 ||
            rect[3] <= 0 ||
            rect[0] + rect[2] < 0 ||
            rect[1] + rect[3] < 0 ||
            rect[0] > size.width ||
            rect[1] > size.height
        ) {
            return@forEach
        }
        inset(rect[0], rect[1], size.width - rect[0] - rect[2], size.height - rect[1] - rect[3]) {
            renderer.draw(this, placed.plant, palette, plantVisuals(placed))
        }
    }
}

/** Geometry only; callers can also position accessible tap targets with these same bounds. */
internal fun worldPlantRect(placed: WorldPlant, state: WorldState, width: Float, height: Float): FloatArray {
    if (state.space == WorldSpace.NORMALIZED) {
        return floatArrayOf(placed.x * width, placed.y * height, placed.width * width, placed.height * height)
    }
    val unit = minOf(width / 6f, height / 4f) * state.camera.zoom
    return floatArrayOf(
        width / 2f + (placed.x - state.camera.x - 0.65f) * unit,
        height * 0.58f + (placed.y - state.camera.y - 1.3f) * unit,
        unit * 1.3f,
        unit * 1.3f
    )
}

/** Shared stage/health mapping; animation overrides are drawing inputs, never stored growth. */
internal fun plantVisuals(placed: WorldPlant): PlantVisuals {
    val plant = placed.plant
    val growth = placed.growth ?: when (plant.stage) {
        PlantStage.SEED -> 0f
        PlantStage.SPROUT -> 0.5f
        PlantStage.BUD -> 0.78f
        PlantStage.BLOOM -> 1f
    }
    return PlantVisuals(
        growth,
        placed.bloom ?: if (plant.stage == PlantStage.BLOOM) 1f else 0f,
        placed.wilt
            ?: if (plant.condition == PlantCondition.GROWING) 0f else ((60 - plant.health) / 50f).coerceIn(0f, 1f),
        placed.swayDegrees
    )
}

private fun DrawScope.drawWorldBackdrop(sceneColors: GardenEnvironmentColors, colors: ColorScheme, light: GardenLight) {
    drawRect(Brush.verticalGradient(listOf(sceneColors.sky, sceneColors.horizon)))
    val ground = Path().apply {
        moveTo(0f, size.height * 0.45f)
        cubicTo(
            size.width * 0.3f,
            size.height * 0.34f,
            size.width * 0.68f,
            size.height * 0.53f,
            size.width,
            size.height * 0.38f
        )
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(ground, sceneColors.ground)
    drawOval(
        colors.onTertiaryContainer.copy(alpha = 0.05f),
        Offset(size.width * 0.62f, size.height * 0.08f),
        Size(size.width * 0.24f, 12.dp.toPx())
    )
    if (light == GardenLight.NIGHT) {
        val moon = Offset(size.width * 0.84f, size.height * 0.11f)
        drawCircle(colors.onTertiaryContainer, 12.dp.toPx(), moon)
        drawCircle(sceneColors.sky, 11.dp.toPx(), moon + Offset(5.dp.toPx(), -3.dp.toPx()))
    }
}
