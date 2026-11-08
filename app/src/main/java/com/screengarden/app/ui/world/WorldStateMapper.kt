// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.world

import com.screengarden.app.domain.garden.GardenLight
import com.screengarden.app.domain.garden.GardenWeather
import com.screengarden.app.domain.world.WorldSpace
import com.screengarden.app.domain.world.WorldState
import com.screengarden.app.domain.world.mapWorldPlants
import com.screengarden.app.ui.screens.garden.GardenUiState

internal fun GardenUiState.toWorldState(): WorldState = WorldState(
    plants = mapWorldPlants(plants),
    environment = if (isBedtime) {
        environment.copy(
            light = GardenLight.NIGHT,
            weather = GardenWeather.CLEAR
        )
    } else {
        environment
    },
    health = score,
    streakDays = streakDays,
    lowPowerVisuals = lowPowerVisuals || isBedtime,
    space = WorldSpace.FIELD
)
