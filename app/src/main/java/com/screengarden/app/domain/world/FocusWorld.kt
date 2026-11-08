// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.world

import com.screengarden.app.domain.focus.FocusSessionState
import com.screengarden.app.domain.garden.Plant

/** Ephemeral display growth only. The persisted session transaction remains the sole reward writer. */
fun WorldState.focusPreview(session: FocusSessionState): WorldState {
    val progress = (session.actualDurationMs.toDouble() / session.plannedDurationMs).toFloat().coerceIn(0f, 1f)
    val chosen = plants.firstOrNull { it.plant.id == session.plantId } ?: plants.lastOrNull()
    val seed =
        chosen ?: WorldPlant(Plant("preview-${session.id}", "fern", plantedAtUtcMillis = session.startedAtUtcMillis))
    val base = when (seed.plant.stage) {
        com.screengarden.app.domain.garden.PlantStage.SEED -> 0f
        com.screengarden.app.domain.garden.PlantStage.SPROUT -> 0.5f
        com.screengarden.app.domain.garden.PlantStage.BUD -> 0.78f
        com.screengarden.app.domain.garden.PlantStage.BLOOM -> 1f
    }
    val target = seed.copy(
        plant = seed.plant.copy(growthPoints = maxOf(seed.plant.growthPoints, (progress * 45).toInt())),
        growth = base + progress * (1f - base),
        bloom = if (base == 1f) 1f else progress
    )
    return copy(
        plants = if (chosen == null) plants + target else plants.map { if (it == chosen) target else it },
        camera = WorldCamera(target.x, target.y, 1.5f)
    )
}
