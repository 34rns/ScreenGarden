// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.annotation.StringRes
import com.screengarden.app.R
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.domain.garden.PlantStage
import java.util.Locale

@StringRes
internal fun plantNameResource(species: String): Int = when (species.lowercase(Locale.ROOT)) {
    "flower" -> R.string.garden_plant_flower
    "bonsai" -> R.string.garden_plant_bonsai
    "cactus" -> R.string.garden_plant_cactus
    "fern" -> R.string.garden_plant_fern
    else -> R.string.garden_plant_unknown
}

@StringRes
internal fun plantStageResource(stage: PlantStage): Int = when (stage) {
    PlantStage.SEED -> R.string.garden_stage_seed
    PlantStage.SPROUT -> R.string.garden_stage_sprout
    PlantStage.BUD -> R.string.garden_stage_bud
    PlantStage.BLOOM -> R.string.garden_stage_bloom
}

@StringRes
internal fun plantConditionResource(condition: PlantCondition): Int = when (condition) {
    PlantCondition.GROWING -> R.string.garden_condition_growing
    PlantCondition.WILTING -> R.string.garden_condition_wilting
    PlantCondition.RECOVERING -> R.string.garden_condition_recovering
}
