// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.domain.garden.Plant
import kotlinx.coroutines.flow.Flow

/**
 * The garden: which plants exist and how they are doing.
 *
 * Room mappings stay inside [GardenRepositoryImpl]; callers use domain plants.
 */
interface GardenRepository {

    fun observePlants(): Flow<List<Plant>>

    suspend fun getPlant(plantId: String): Plant?

    suspend fun savePlant(plant: Plant)

    suspend fun deletePlant(plantId: String)
}
