// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.data.local.entity.Plant
import kotlinx.coroutines.flow.Flow

/**
 * The garden: which plants exist and how they are doing.
 *
 * INTERFACE ONLY for now (Task 1.1); Phase 1.4 renders it and Phase 1.5 owns the
 * growth rules that write to it.
 */
interface GardenRepository {

    fun observePlants(): Flow<List<Plant>>

    suspend fun getPlant(plantId: String): Plant?

    suspend fun savePlant(plant: Plant)

    suspend fun deletePlant(plantId: String)
}
