// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import androidx.room.withTransaction
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.data.local.entity.DailyStat
import com.screengarden.app.data.local.entity.Plant as PlantEntity
import com.screengarden.app.domain.garden.GardenDay
import com.screengarden.app.domain.garden.GardenDayRecord
import com.screengarden.app.domain.garden.GardenEngine
import com.screengarden.app.domain.garden.GardenSnapshot
import com.screengarden.app.domain.garden.GardenStore
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.domain.garden.PlantTuning
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Room-backed garden with atomic snapshots and once-only completed-day rewards. */
class GardenRepositoryImpl(private val database: ScreenGardenDatabase) :
    GardenRepository,
    GardenStore {

    private val plants = database.plantDao()
    private val days = database.dailyStatDao()

    override fun observePlants(): Flow<List<Plant>> = plants.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getPlant(plantId: String): Plant? = plants.getById(plantId)?.toDomain()

    override suspend fun savePlant(plant: Plant) {
        require(plant.id.isNotBlank() && plant.species.isNotBlank()) { "A plant needs an id and species" }
        plants.upsert(plant.toEntity())
    }

    override suspend fun deletePlant(plantId: String) {
        plants.deleteById(plantId)
    }

    override fun observeSnapshot(fromDayStartUtcMillis: Long, toDayStartUtcMillis: Long): Flow<GardenSnapshot> =
        combine(
            plants.observeAll(),
            days.observeRange(fromDayStartUtcMillis, toDayStartUtcMillis),
            database.appLimitDao().observeAll(),
            database.appUsageDailyDao().observeRange(fromDayStartUtcMillis, toDayStartUtcMillis)
        ) { _, _, _, _ ->
            // Separate table flows can emit in either order; re-read their shared committed state.
            database.withTransaction {
                val enabled = database.appLimitDao().getAll().filter { it.isEnabled }
                val usage = database.appUsageDailyDao().getRange(fromDayStartUtcMillis, toDayStartUtcMillis)
                    .associateBy { it.dayStartUtcMillis to it.packageName }
                GardenSnapshot(
                    plants = plants.getAll().map { it.toDomain() },
                    days = days.getRange(fromDayStartUtcMillis, toDayStartUtcMillis).map { row ->
                        row.toDomain().copy(
                            limitOveragesMinutes = enabled.takeIf { it.isNotEmpty() }?.map { limit ->
                                val used = usage[row.dayStartUtcMillis to limit.packageName]?.usageMillis ?: 0L
                                (used / 60_000L - limit.dailyLimitMinutes).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
                            }
                        )
                    }
                )
            }
        }.distinctUntilChanged()

    override suspend fun recordScore(dayStartUtcMillis: Long, score: Int?, expectedUpdatedAtUtcMillis: Long): Boolean {
        require(score == null || score in 0..100) { "A score must be inside 0..100 or unknown" }
        return database.withTransaction {
            days.updateScore(dayStartUtcMillis, score, expectedUpdatedAtUtcMillis) == 1
        }
    }

    override suspend fun advanceCompletedDay(day: GardenDay): Boolean = database.withTransaction {
        val score = day.score ?: return@withTransaction false
        if (score !in 0..100) return@withTransaction false
        val recordedDay = days.getByDay(day.dayStartUtcMillis) ?: return@withTransaction false
        if (!recordedDay.usageIsBounded || recordedDay.gardenApplied || recordedDay.score != score) {
            return@withTransaction false
        }
        val previousPlants = plants.getAll().map { it.toDomain() }
        val outcome = GardenEngine.advance(day, previousPlants)
        val previousStages = previousPlants.associate { it.id to it.stage }
        val plantsGrown = outcome.plants.count { plant ->
            previousStages[plant.id]?.let { it != plant.stage } == true
        }
        outcome.plants.forEach { plants.upsert(it.toEntity()) }
        check(days.markGardenApplied(day.dayStartUtcMillis, score, plantsGrown) == 1) {
            "Completed day changed during its garden transaction"
        }
        true
    }
}

internal fun PlantEntity.toDomain(): Plant = Plant(
    id = plantId,
    species = species,
    growthPoints = growthPoints.coerceAtLeast(0),
    health = health.coerceIn(PlantTuning.MIN_HEALTH, PlantTuning.MAX_HEALTH),
    condition = PlantCondition.entries.firstOrNull { it.name == condition }
        ?: if (health >= PlantTuning.HEALTHY_AT) PlantCondition.GROWING else PlantCondition.WILTING,
    plantedAtUtcMillis = plantedAtUtcMillis,
    lastFocusAtUtcMillis = lastFocusAtUtcMillis
)

internal fun Plant.toEntity(): PlantEntity = PlantEntity(
    plantId = id,
    species = species,
    stage = stage.ordinal,
    health = health.coerceIn(PlantTuning.MIN_HEALTH, PlantTuning.MAX_HEALTH),
    plantedAtUtcMillis = plantedAtUtcMillis,
    lastFocusAtUtcMillis = lastFocusAtUtcMillis,
    growthPoints = growthPoints.coerceAtLeast(0),
    condition = condition.name
)

private fun DailyStat.toDomain(): GardenDayRecord = GardenDayRecord(
    dayStartUtcMillis = dayStartUtcMillis,
    score = score,
    screenMinutes = screenMinutes,
    focusMinutes = focusMinutes,
    sessionsCompleted = sessionsCompleted,
    pickups = pickups,
    unlocks = unlocks,
    usageIsBounded = usageIsBounded,
    gardenApplied = gardenApplied,
    updatedAtUtcMillis = updatedAtUtcMillis,
    bedtimeMinutes = bedtimeMinutes
)
