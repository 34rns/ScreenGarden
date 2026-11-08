// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

/** Versioned, local artwork metadata. Paths are validated by the pack loader. */
data class PlantPack(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val license: String,
    val unlock: PlantUnlock,
    val stageImages: Map<PlantStage, String>
)

/** Unlocks affect new seeds only; existing plants never lose their artwork or progress. */
sealed interface PlantUnlock {
    data object Default : PlantUnlock
    data class Streak(val days: Int) : PlantUnlock
    data class Focus(val completions: Int) : PlantUnlock
    data class Seasonal(val season: Season) : PlantUnlock

    fun isUnlocked(streakDays: Int, completedFocus: Int, season: Season): Boolean = when (this) {
        Default -> true
        is Streak -> streakDays >= days
        is Focus -> completedFocus >= completions
        is Seasonal -> this.season == season
    }
}

/** Stable variety without another preference or a new database field. */
fun chooseSeedSpecies(
    packs: List<PlantPack>,
    existingPlants: Int,
    streakDays: Int,
    completedFocus: Int,
    season: Season
): String {
    val eligible = packs.filter { it.unlock.isUnlocked(streakDays, completedFocus, season) }
        .sortedWith(compareBy<PlantPack> { it.unlock != PlantUnlock.Default }.thenBy { it.id })
    return if (eligible.isEmpty()) {
        GardenTuning.DEFAULT_SPECIES
    } else {
        eligible[
            existingPlants.coerceAtLeast(0) %
                eligible.size
        ].id
    }
}
