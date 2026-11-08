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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
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
import com.screengarden.app.domain.world.RenderMode
import com.screengarden.app.domain.world.WorldPlant
import com.screengarden.app.domain.world.WorldState
import com.screengarden.app.ui.theme.gardenEnvironmentColors
import com.screengarden.app.ui.world.drawWorld
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
                    drawWorld(
                        WorldState(
                            environment = scene,
                            phase = phase.value,
                            streakDays = if (plants.isEmpty()) 0 else streakDays.coerceAtLeast(1),
                            lowPowerVisuals = lowPowerVisuals
                        ),
                        RenderMode.INTERACTIVE,
                        colors,
                        VectorPlantRenderer
                    )
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
        drawWorld(
            WorldState(
                plants = listOf(
                    WorldPlant(
                        plant,
                        growth = growth.value,
                        bloom = bloom.value,
                        wilt = wilt.value,
                        swayDegrees = sway
                    )
                ),
                backdrop = false
            ),
            RenderMode.INTERACTIVE,
            colors,
            renderer,
            palette
        )
    }
}
