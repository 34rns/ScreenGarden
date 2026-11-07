// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import com.screengarden.app.domain.garden.Plant

/** A single snapshot of the garden. Missing measurements never become a zero score. */
data class GardenUiState(
    val loading: Boolean = true,
    val availability: GardenAvailability = GardenAvailability.NO_DATA,
    val plants: List<Plant> = emptyList(),
    val score: Int? = null,
    val streakDays: Int = 0,
    val error: Boolean = false
)

/** Normal data availability, independent of saved plants and recoverable read errors. */
enum class GardenAvailability {
    READY,
    NO_PERMISSION,
    NO_DATA,
    INCOMPLETE_USAGE
}
