// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.screengarden.app.domain.garden.GardenEnvironment
import com.screengarden.app.domain.garden.GardenLight
import com.screengarden.app.domain.garden.GardenWeather
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.domain.garden.PlantStage
import com.screengarden.app.domain.world.RenderMode
import com.screengarden.app.domain.world.WorldPlant
import com.screengarden.app.domain.world.WorldSpace
import com.screengarden.app.domain.world.WorldState
import com.screengarden.app.domain.world.mapWorldPlants
import com.screengarden.app.ui.world.WorldScene
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
    val scene = if (isBedtime) {
        environment.copy(
            light = GardenLight.NIGHT,
            weather = GardenWeather.CLEAR
        )
    } else {
        environment
    }
    WorldScene(
        WorldState(
            plants = mapWorldPlants(plants),
            environment = scene,
            streakDays = streakDays,
            lowPowerVisuals = lowPowerVisuals || isBedtime,
            space = WorldSpace.FIELD
        ),
        modifier,
        onPlantSelected = onPlantSelected
    )
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
