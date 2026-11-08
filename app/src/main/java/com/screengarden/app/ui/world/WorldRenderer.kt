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
import com.screengarden.app.domain.world.RenderMode
import com.screengarden.app.domain.world.WorldPlant
import com.screengarden.app.domain.world.WorldSpace
import com.screengarden.app.domain.world.WorldState
import com.screengarden.app.domain.world.motion
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
        if (state.space == WorldSpace.FIELD) {
            drawMeadow(state, mode, sceneColors, colors)
        } else {
            drawWorldBackdrop(sceneColors, colors, state.environment.light)
        }
        if (mode.policy.maxParticles > 0 && mode != RenderMode.DESK) {
            val motes = if (state.streakDays > 0 && state.environment.light != GardenLight.NIGHT) 6 else 0
            drawGardenWeather(
                state.environment.weather,
                state.phase,
                colors.onTertiaryContainer,
                state.lowPowerVisuals,
                (mode.policy.maxParticles - motes).coerceIn(0, 12)
            )
        }
        if (mode != RenderMode.DESK &&
            state.streakDays > 0 &&
            !state.lowPowerVisuals &&
            state.environment.light != GardenLight.NIGHT
        ) {
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
    val palette = plantPalette ?: if (mode == RenderMode.DESK) {
        PlantPalette(
            com.screengarden.app.ui.theme.DeskFoliage,
            com.screengarden.app.ui.theme.DeskFoliage,
            com.screengarden.app.ui.theme.DeskFoliage,
            com.screengarden.app.ui.theme.DeskBloom,
            com.screengarden.app.ui.theme.DeskFoliage,
            com.screengarden.app.ui.theme.DeskFoliage
        )
    } else {
        PlantPalette(
            colors.primary,
            colors.secondary,
            colors.secondary,
            colors.tertiary,
            colors.onTertiary,
            colors.onSecondaryContainer
        )
    }
    visibleWorldPlants(state, mode, size.width, size.height).forEach { placed ->
        val rect = worldPlantRect(placed, state, size.width, size.height)
        inset(rect[0], rect[1], size.width - rect[0] - rect[2], size.height - rect[1] - rect[3]) {
            val sway = if (state.animationsEnabled) {
                sin(
                    (
                        state.phase +
                            Math.floorMod(placed.plant.id.hashCode(), 100) / 100f
                        ) * (2 * PI).toFloat()
                ) * 1.5f
            } else {
                placed.swayDegrees
            }
            renderer.draw(this, placed.plant, palette, plantVisuals(placed.copy(swayDegrees = sway)))
        }
    }
}

/** Cull before the rendering cap, so distant older and imported plants remain reachable. */
internal fun visibleWorldPlants(state: WorldState, mode: RenderMode, width: Float, height: Float): List<WorldPlant> =
    state.plants.asSequence().filter {
        val r = worldPlantRect(it, state, width, height)
        r.all { value -> value.isFinite() } &&
            r[2] > 0 &&
            r[3] > 0 &&
            r[0] + r[2] >= 0 &&
            r[1] + r[3] >= 0 &&
            r[0] <= width &&
            r[1] <= height &&
            // Field plants are rooted in the foreground, never floating above the distant hills.
            (state.space != WorldSpace.FIELD || r[1] + r[3] >= height * 0.49f) &&
            // Keep Android's wallpaper dock area clear, including large collections.
            (mode != RenderMode.WALLPAPER || r[1] + r[3] <= height * 0.8f)
    }.take(mode.policy.maxPlants).toList().sortedWith(compareBy<WorldPlant> { it.y }.thenBy { it.x })

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
    val motion = placed.motion()
    return PlantVisuals(motion.growth, motion.bloom, motion.wilt, placed.swayDegrees)
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

/** Original layered meadow; details are deterministic and never request an art/weather service. */
private fun DrawScope.drawMeadow(
    state: WorldState,
    mode: RenderMode,
    scene: GardenEnvironmentColors,
    colors: ColorScheme
) {
    val desk = mode == RenderMode.DESK
    val sky = if (desk) com.screengarden.app.ui.theme.DeskSky else scene.sky
    val ground = if (desk) com.screengarden.app.ui.theme.DeskGround else scene.ground
    drawRect(Brush.verticalGradient(listOf(sky, if (desk) sky else scene.horizon)))
    fun hill(top: Float, bend: Float, ink: androidx.compose.ui.graphics.Color) {
        val path = Path().apply {
            moveTo(0f, size.height * top)
            cubicTo(
                size.width * 0.25f,
                size.height * (top - bend),
                size.width * 0.75f,
                size.height * (top + bend),
                size.width,
                size.height * (top - 0.04f)
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, ink)
    }
    hill(0.35f, 0.16f, androidx.compose.ui.graphics.lerp(sky, ground, 0.55f))
    hill(0.43f, -0.08f, androidx.compose.ui.graphics.lerp(sky, ground, 0.8f))
    hill(0.49f, 0.09f, ground)
    // Small grove beyond the cultivated field; no texture or bitmap allocations.
    repeat(if (desk) 4 else 9) { index ->
        val x = size.width * (index * 0.12f + 0.02f)
        val y = size.height * (0.38f + sin(index * 1.8f) * 0.025f)
        val radius = size.width * (0.015f + (index % 3) * 0.004f)
        val ink = if (desk) com.screengarden.app.ui.theme.DeskFoliage else colors.secondary.copy(alpha = 0.25f)
        drawLine(ink, Offset(x, y), Offset(x, y + radius * 2), radius * 0.22f)
        drawCircle(ink, radius, Offset(x, y - radius * 0.3f))
        drawCircle(ink, radius * 0.7f, Offset(x + radius * 0.6f, y))
    }
    if (!desk && !state.lowPowerVisuals) {
        repeat(24) { index ->
            val x = size.width * ((index * 0.137f) % 1f)
            val y = size.height * (0.58f + (index * 0.073f) % 0.38f)
            drawOval(colors.primary.copy(alpha = 0.09f), Offset(x, y), Size(size.width * 0.025f, size.height * 0.006f))
        }
    }
}
