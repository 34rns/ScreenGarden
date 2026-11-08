// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.garden.GardenEnvironment
import com.screengarden.app.domain.garden.GardenLight
import com.screengarden.app.domain.garden.GardenWeather
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.domain.garden.PlantStage
import com.screengarden.app.ui.theme.GardenEnvironmentColors
import com.screengarden.app.ui.theme.gardenEnvironmentColors
import kotlin.math.PI
import kotlin.math.sin

/** A scroll-friendly plot with native, keyboard and screen-reader accessible plant controls. */
@Composable
fun GardenScene(
    plants: List<Plant>,
    onPlantSelected: (Plant) -> Unit,
    modifier: Modifier = Modifier,
    streakDays: Int = 0,
    isBedtime: Boolean = false,
    environment: GardenEnvironment = GardenEnvironment(),
    lowPowerVisuals: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val scene = if (isBedtime) {
        environment.copy(
            light = GardenLight.NIGHT,
            weather = GardenWeather.CLEAR
        )
    } else {
        environment
    }
    val sceneColors = gardenEnvironmentColors(scene, colors)
    val skyBrush = remember(sceneColors) { Brush.verticalGradient(listOf(sceneColors.sky, sceneColors.horizon)) }
    val motionEnabled = rememberGardenMotionEnabled() && !isBedtime && !lowPowerVisuals
    val phase = rememberGardenPhase(motionEnabled && plants.isNotEmpty())
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = sceneColors.sky
    ) {
        BoxWithConstraints {
            val columns = if (maxWidth >= 340.dp) 3 else 2
            Box {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawGardenBackdrop(sceneColors, colors, scene.light, skyBrush)
                    drawGardenWeather(scene.weather, phase.value, colors.onTertiaryContainer, lowPowerVisuals)
                    if (plants.isNotEmpty()) {
                        val time = phase.value
                        if (!lowPowerVisuals && scene.light != GardenLight.NIGHT) {
                            repeat(6) { index ->
                                val x = size.width * ((index * 0.17f + time * 0.08f) % 1f)
                                val y = size.height * (0.15f + index * 0.07f) +
                                    sin((time * 2 * PI + index).toFloat()) * 4.dp.toPx()
                                drawCircle(colors.onTertiaryContainer.copy(alpha = 0.12f), 1.5.dp.toPx(), Offset(x, y))
                            }
                        }
                        if (streakDays >= 3 && scene.light != GardenLight.NIGHT && !lowPowerVisuals) {
                            val center = Offset(size.width * (0.18f + time * 0.6f), size.height * 0.16f)
                            val wing = 2.dp.toPx() + kotlin.math.abs(sin(time * 12 * PI).toFloat()) * 3.dp.toPx()
                            drawOval(colors.tertiary, center - Offset(wing, 3.dp.toPx()), Size(wing, 6.dp.toPx()))
                            drawOval(colors.tertiary, center, Size(wing, 6.dp.toPx()))
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 184.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    plants.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { plant ->
                                key(plant.id) {
                                    PlantTarget(
                                        plant = plant,
                                        onClick = { onPlantSelected(plant) },
                                        modifier = Modifier.weight(1f),
                                        motionEnabled = motionEnabled,
                                        phase = phase
                                    )
                                }
                            }
                            repeat(columns - row.size) { Box(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

/** Shared by the live scene and exported static image. */
internal fun DrawScope.drawGardenBackdrop(
    sceneColors: GardenEnvironmentColors,
    colors: ColorScheme,
    light: GardenLight,
    skyBrush: Brush
) {
    drawRect(skyBrush)
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

@Composable
private fun PlantTarget(
    plant: Plant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean,
    phase: State<Float>
) {
    val name = stringResource(plantNameResource(plant.species))
    val stage = stringResource(plantStageResource(plant.stage))
    val condition = stringResource(plantConditionResource(plant.condition))
    val description = stringResource(R.string.garden_plant_description, name, stage)
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 144.dp)
            .testTag("garden_plant_${plant.id}")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = condition
                role = Role.Button
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column {
            GardenPlantArt(plant, Modifier.fillMaxWidth().height(108.dp), motionEnabled, phase)
            Text(
                text = name,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).clearAndSetSemantics { },
                style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Content),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun GardenPlantArt(
    plant: Plant,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean = false,
    phase: State<Float>? = null
) {
    val colors = MaterialTheme.colorScheme
    val palette = PlantPalette(
        leaf = colors.primary,
        mutedLeaf = colors.secondary,
        stem = colors.secondary,
        bloom = colors.tertiary,
        bloomCenter = colors.onTertiary,
        soil = colors.onSecondaryContainer
    )
    val renderer = LocalPlantRenderer.current
    val animation = if (motionEnabled) tween<Float>(900) else snap<Float>()
    val growth = animateFloatAsState(
        when (plant.stage) {
            PlantStage.SEED -> 0f
            PlantStage.SPROUT -> 0.5f
            PlantStage.BUD -> 0.78f
            PlantStage.BLOOM -> 1f
        },
        animation,
        label = "plant growth"
    )
    val bloom = animateFloatAsState(if (plant.stage == PlantStage.BLOOM) 1f else 0f, animation, label = "bloom opening")
    val wilt = animateFloatAsState(
        if (plant.condition != PlantCondition.GROWING) {
            ((60 - plant.health) / 50f).coerceIn(0f, 1f)
        } else {
            0f
        },
        animation,
        label = "plant recovery"
    )
    Canvas(modifier = modifier) {
        val offset = Math.floorMod(plant.id.hashCode(), 100) / 100f
        val sway = if (motionEnabled) sin(((phase?.value ?: 0f) + offset) * (2 * PI).toFloat()) * 2f else 0f
        renderer.draw(this, plant, palette, PlantVisuals(growth.value, bloom.value, wilt.value, sway))
    }
}
