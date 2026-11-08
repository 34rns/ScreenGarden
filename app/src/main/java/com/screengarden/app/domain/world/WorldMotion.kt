// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.world

import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.domain.garden.PlantStage

/** Presentation values independent of Android, clocks and persisted rewards. */
data class WorldPlantMotion(val growth: Float, val bloom: Float, val wilt: Float)

fun WorldPlant.motion(): WorldPlantMotion = WorldPlantMotion(
    growth
        ?: when (plant.stage) {
            PlantStage.SEED -> 0f
            PlantStage.SPROUT -> 0.5f
            PlantStage.BUD -> 0.78f
            PlantStage.BLOOM -> 1f
        },
    bloom ?: if (plant.stage == PlantStage.BLOOM) 1f else 0f,
    wilt ?: if (plant.condition == PlantCondition.GROWING) 0f else ((60 - plant.health) / 50f).coerceIn(0f, 1f)
)

/** Explicit elapsed time makes stage opening, wilt and recovery deterministic and testable. */
class WorldVisualTransition(from: List<WorldPlant>, private val target: List<WorldPlant>) {
    private val previous = from.associate { it.plant.id to it.motion() }
    fun sample(elapsedMs: Long, moving: Boolean): List<WorldPlant> {
        val fraction = if (moving) (elapsedMs / 900f).coerceIn(0f, 1f) else 1f
        return target.map { placed ->
            val next = placed.motion()
            val old = previous[placed.plant.id] ?: next
            fun mix(before: Float, after: Float): Float = before + (after - before) * fraction
            placed.copy(
                growth = mix(old.growth, next.growth),
                bloom = mix(old.bloom, next.bloom),
                wilt = mix(old.wilt, next.wilt)
            )
        }
    }
}
