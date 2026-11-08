// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.world

import com.screengarden.app.domain.garden.GardenEnvironment
import com.screengarden.app.domain.garden.Plant
import kotlin.math.max

/** A plant's drawing location and optional ephemeral animation; never a reward write. */
data class WorldPlant(
    val plant: Plant,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 1f,
    val height: Float = 1f,
    val growth: Float? = null,
    val bloom: Float? = null,
    val wilt: Float? = null,
    val swayDegrees: Float = 0f
)

enum class WorldSpace { NORMALIZED, FIELD }

/** Immutable world input. Drawing owns no clock, database, permission or platform state. */
data class WorldState(
    val plants: List<WorldPlant> = emptyList(),
    val environment: GardenEnvironment = GardenEnvironment(),
    val camera: WorldCamera = WorldCamera(),
    val health: Int? = null,
    val streakDays: Int = 0,
    val phase: Float = 0f,
    val animationsEnabled: Boolean = false,
    val lowPowerVisuals: Boolean = false,
    val backdrop: Boolean = true,
    val space: WorldSpace = WorldSpace.NORMALIZED
) {
    val bounds: WorldBounds get() = if (plants.isEmpty()) {
        WorldBounds(-3f, -2f, 3f, 2f)
    } else {
        WorldBounds(
            plants.minOf { it.x } - 3f,
            plants.minOf { it.y } - 2f,
            plants.maxOf { it.x } + 3f,
            plants.maxOf { it.y } + 2f
        )
    }
}

/** Chronological square spiral: new plants append; old plants never disappear or overlap. */
fun mapWorldPlants(plants: List<Plant>): List<WorldPlant> {
    var x = 0
    var y = 0
    var dx = 1
    var dy = 0
    var segment = 1
    var progress = 0
    var turns = 0
    return plants.sortedWith(compareBy<Plant> { it.plantedAtUtcMillis }.thenBy { it.id }).map { plant ->
        val mapped = WorldPlant(plant, x * 1.6f, y * 1.1f)
        x += dx
        y += dy
        progress++
        if (progress == segment) {
            progress = 0
            val oldDx = dx
            dx = -dy
            dy = oldDx
            turns++
            if (turns % 2 == 0) segment++
        }
        mapped
    }
}

/** Legacy framing used during extraction, so moving drawing cannot silently move existing art. */
fun mapPlotPlants(plants: List<Plant>, plotFraction: Float = 0.65f): List<WorldPlant> {
    val columns = plants.size.coerceIn(1, 3)
    val rows = max(1, (plants.size + columns - 1) / columns)
    return plants.mapIndexed { index, plant ->
        WorldPlant(
            plant,
            (index % columns).toFloat() / columns,
            0.3f + index / columns * plotFraction / rows,
            1f / columns,
            plotFraction / rows
        )
    }
}
