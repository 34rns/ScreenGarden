// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import com.screengarden.app.data.local.dao.GardenHistoryDao
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.domain.garden.PlantTuning
import com.screengarden.app.domain.insights.GardenHistoryFrame
import com.screengarden.app.domain.insights.GardenHistorySnapshot
import com.screengarden.app.domain.insights.GardenHistoryStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** Invalid stored frames are reported as unavailable instead of substituted with an invented garden. */
class GardenHistoryRepository(private val dao: GardenHistoryDao) : GardenHistoryStore {
    override fun observe(): Flow<GardenHistorySnapshot> = dao.observeAll().map { rows ->
        val frames = rows.mapNotNull { row ->
            runCatching {
                require(row.score in 0..100)
                GardenHistoryFrame(
                    row.dayStartUtcMillis,
                    row.capturedAtUtcMillis,
                    row.score,
                    GardenHistoryCodec.decode(row.plantsJson)
                )
            }.getOrNull()
        }
        GardenHistorySnapshot(frames, rows.size - frames.size)
    }
}

/** Small versioned payload for app-owned immutable snapshots; no extra JSON dependency. */
internal object GardenHistoryCodec {
    fun encode(plants: List<Plant>): String {
        val values = JSONArray()
        plants.forEach { plant ->
            values.put(
                JSONObject().put("id", plant.id).put("species", plant.species)
                    .put("growthPoints", plant.growthPoints).put("health", plant.health)
                    .put("condition", plant.condition.name).put("plantedAtUtcMillis", plant.plantedAtUtcMillis)
                    .put("lastFocusAtUtcMillis", plant.lastFocusAtUtcMillis ?: JSONObject.NULL)
            )
        }
        return JSONObject().put("version", 1).put("plants", values).toString()
    }

    fun decode(json: String): List<Plant> {
        require(json.length <= 1_000_000)
        val value = JSONObject(json)
        require(value.getInt("version") == 1)
        val plants = value.getJSONArray("plants")
        require(plants.length() <= 1000)
        val result = (0 until plants.length()).map { index ->
            val plant = plants.getJSONObject(index)
            Plant(
                plant.getString("id"),
                plant.getString("species"),
                plant.getInt("growthPoints"),
                plant.getInt("health"),
                PlantCondition.valueOf(plant.getString("condition")),
                plant.getLong("plantedAtUtcMillis"),
                if (plant.isNull("lastFocusAtUtcMillis")) null else plant.getLong("lastFocusAtUtcMillis")
            )
                .also {
                    require(it.id.isNotBlank() && it.species.isNotBlank() && it.growthPoints >= 0)
                    require(it.health in PlantTuning.MIN_HEALTH..PlantTuning.MAX_HEALTH)
                }
        }
        require(result.map { it.id }.distinct().size == result.size)
        return result
    }
}
