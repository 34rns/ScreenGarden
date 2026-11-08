// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

import com.screengarden.app.domain.focus.FocusStatus
import java.util.UUID
import kotlin.math.roundToInt

/**
 * The rules that turn a day into garden movement, as constants (AGENT_MEMORY 16.2).
 *
 * Growth is deliberately slow: a perfect day is worth about five points, so a
 * seed needs nine perfect days to bloom without other rewards. The app is meant to feel calm,
 * and a garden that fills up in two days has nothing left to say.
 */
object GardenTuning {

    /** Lifetime plant ledger; P35 caps only visible rendering, never removes older plantings. */
    const val MAX_PLANTS: Int = Int.MAX_VALUE

    /** Ceiling on growth per day, however good the day was. */
    const val MAX_GROWTH_PER_DAY: Int = 10

    /** Growth points a perfect (100) day is worth. */
    const val GROWTH_PER_FULL_SCORE: Int = 5

    /** Growth per counted focus session. */
    const val GROWTH_PER_FOCUS: Int = 2

    /** Focus sessions that still count after the first ones; more is not better. */
    const val MAX_COUNTED_FOCUS: Int = 2

    /** A streak starts paying a bonus on this day. */
    const val STREAK_BONUS_FROM_DAYS: Int = 3

    /** Ceiling on the streak bonus, so a long streak does not dominate. */
    const val MAX_STREAK_BONUS: Int = 3

    /** A day at or above this score counts as a good day. */
    const val GOOD_DAY_SCORE: Int = 60

    /** A day below this score is a poor day and wilts the garden. */
    const val POOR_DAY_SCORE: Int = 40

    /** Health a good day adds. */
    const val HEALTH_GAIN_PER_GOOD_DAY: Int = 10

    /** Health a poor day takes away: slow enough to wilt over a week, not a day. */
    const val WILT_PER_POOR_DAY: Int = 6

    /** Health a counted focus session adds, so focus can rescue a bad week. */
    const val HEALTH_PER_FOCUS: Int = 4

    /** Species given to a seed planted by the engine until plant packs land (P25). */
    const val DEFAULT_SPECIES: String = "fern"
}

/**
 * Every tunable number the engine uses, validated in one place.
 *
 * A data class rather than bare constants so a test can pin an edge case (a cap
 * of one plant, a different growth rate) without editing the rules everyone else uses,
 * while [GardenTuning] stays the source of the shipped defaults.
 */
data class GardenRules(
    val maxPlants: Int = GardenTuning.MAX_PLANTS,
    val maxGrowthPerDay: Int = GardenTuning.MAX_GROWTH_PER_DAY,
    val growthPerFullScore: Int = GardenTuning.GROWTH_PER_FULL_SCORE,
    val growthPerFocus: Int = GardenTuning.GROWTH_PER_FOCUS,
    val maxCountedFocus: Int = GardenTuning.MAX_COUNTED_FOCUS,
    val streakBonusFromDays: Int = GardenTuning.STREAK_BONUS_FROM_DAYS,
    val maxStreakBonus: Int = GardenTuning.MAX_STREAK_BONUS,
    val goodDayScore: Int = GardenTuning.GOOD_DAY_SCORE,
    val poorDayScore: Int = GardenTuning.POOR_DAY_SCORE,
    val healthGainPerGoodDay: Int = GardenTuning.HEALTH_GAIN_PER_GOOD_DAY,
    val wiltPerPoorDay: Int = GardenTuning.WILT_PER_POOR_DAY,
    val healthPerFocus: Int = GardenTuning.HEALTH_PER_FOCUS,
    val healthyAt: Int = PlantTuning.HEALTHY_AT,
    val minHealth: Int = PlantTuning.MIN_HEALTH,
    val maxHealth: Int = PlantTuning.MAX_HEALTH,
    val newPlantHealth: Int = PlantTuning.NEW_PLANT_HEALTH,
    val species: String = GardenTuning.DEFAULT_SPECIES
) {

    init {
        require(maxPlants >= 1) { "A garden must hold at least one plant" }
        require(maxGrowthPerDay >= 0 && growthPerFullScore >= 0 && growthPerFocus >= 0 && maxCountedFocus >= 0) {
            "Growth settings must not be negative"
        }
        require(streakBonusFromDays >= 1) { "streakBonusFromDays must be at least 1" }
        require(maxStreakBonus >= 0) { "maxStreakBonus must not be negative" }
        require(goodDayScore in 0..100 && poorDayScore in 0..goodDayScore) {
            "Scores must be inside 0..100 and poorDayScore must not exceed goodDayScore"
        }
        require(healthGainPerGoodDay >= 0 && wiltPerPoorDay >= 0 && healthPerFocus >= 0) {
            "Health settings must not be negative"
        }
        require(maxHealth in 1..100 && minHealth in 1..maxHealth) { "Health bounds must be inside 1..100" }
        require(healthyAt in minHealth..maxHealth) { "healthyAt must sit between the health bounds" }
        require(newPlantHealth in minHealth..maxHealth) { "newPlantHealth must sit between the health bounds" }
        require(species.isNotBlank()) { "A plant needs a species" }
    }
}

/**
 * One day of inputs for the engine: the score P08 produced and the things the
 * engine cannot read from a score.
 *
 * [score] is nullable for the same reason [HealthScoreCalculator] returns null:
 * a day with no usage access or no goals is unknown, and an unknown day must not
 * wilt a garden (K49 stays honest).
 */
data class GardenDay(
    val score: Int?,
    val focusSessionsCompleted: Int = 0,
    val streakDays: Int = 0,
    val dayStartUtcMillis: Long = 0L,
    /** Actual latest completed-session timestamp; null when it is not known. */
    val lastFocusAtUtcMillis: Long? = null,
    /** P14 terminal transactions already delivered these focus points and healing. */
    val focusRewardsAlreadyApplied: Boolean = false
)

/**
 * What one [GardenEngine.advance] did.
 *
 * The event lists exist so the UI can animate exactly what happened instead of
 * diffing two gardens (P11), and so a test can assert "this wilted today" without
 * comparing health numbers.
 */
data class GardenOutcome(
    val plants: List<Plant>,
    val planted: Plant? = null,
    val bloomed: List<String> = emptyList(),
    val wilted: List<String> = emptyList(),
    val recovered: List<String> = emptyList()
) {

    /** The plant with [id] in the new garden, or null when it does not exist. */
    fun plant(id: String): Plant? = plants.firstOrNull { it.id == id }
}

/**
 * Advances a garden by one day (AGENT_MEMORY 16.2).
 *
 * The rules, in plain language:
 *  - Growth never goes backwards. A day adds points from the score (up to five for
 *    a perfect day), from the first two completed focus sessions, and from a streak
 *    of three days or more, capped at ten points a day in total.
 *  - Health is what a bad day costs. A day at or above 60 adds ten; a day below 40
 *    takes six; between the two nothing changes. Each counted focus session adds
 *    four and forgives that day's loss, so one completion can start recovery.
 *  - Health lives between 10 and 100. The floor is the whole "never permanently
 *    lost" guarantee: no number of poor days can take a plant away, and one good
 *    day starts the climb back. Wilting takes about seven poor days from full
 *    health, which is "gradual over several days" rather than a cliff.
 *  - A day at or above 60 plants one new seed under the caller's optional cap.
 *    The default world retains all plants and grows one planting at a time;
 *    graphics caps are independent of this permanent ledger.
 *  - An unknown score gives no score or streak growth and costs no health; focus
 *    still grows and heals, because completions are known even without usage data.
 *
 * Everything here is pure: no Android import, no clock, no database, and the new
 * plant's id comes from an injectable factory so tests are deterministic.
 */
object GardenEngine {

    /** Delivered atomically with a terminal session: first two completions heal; soft fails cost at most one point. */
    fun applyFocus(
        status: FocusStatus,
        plants: List<Plant>,
        endedAtUtcMillis: Long,
        completedEarlierToday: Int = 0,
        rules: GardenRules = GardenRules(),
        newPlantId: () -> String = { UUID.randomUUID().toString() }
    ): GardenOutcome {
        if (status != FocusStatus.COMPLETED && status != FocusStatus.FAILED_SOFT) return GardenOutcome(plants)
        if (status == FocusStatus.COMPLETED && completedEarlierToday >= rules.maxCountedFocus) {
            return GardenOutcome(
                plants.map { plant ->
                    plant.copy(
                        lastFocusAtUtcMillis = maxOf(plant.lastFocusAtUtcMillis ?: endedAtUtcMillis, endedAtUtcMillis)
                    )
                }
            )
        }
        val focusDay = GardenDay(null, focusSessionsCompleted = 1, lastFocusAtUtcMillis = endedAtUtcMillis)
        val planted = if (plants.isEmpty() && status == FocusStatus.COMPLETED) {
            val id = newPlantId()
            require(id.isNotBlank()) { "A new plant needs a nonblank id" }
            newSeed(focusDay, rules, id).copy(plantedAtUtcMillis = endedAtUtcMillis)
        } else {
            null
        }
        val previous = if (planted == null) plants else listOf(planted)
        val updated = previous.map { plant ->
            if (status == FocusStatus.COMPLETED) {
                grow(plant, focusDay, rules)
            } else {
                // A distraction cannot erase a stage, reduce health or permanently remove a plant.
                plant.copy(
                    growthPoints = (plant.growthPoints.coerceAtLeast(0) - 1)
                        .coerceAtLeast(plant.stage.minimumGrowthPoints)
                )
            }
        }
        return GardenOutcome(
            plants = updated,
            planted = planted?.let { seed -> updated.first { it.id == seed.id } },
            bloomed = updated.filter { plant ->
                previous.first { it.id == plant.id }.stage != PlantStage.BLOOM && plant.stage == PlantStage.BLOOM
            }.map { it.id },
            recovered = updated.filter { plant ->
                previous.first { it.id == plant.id }.condition == PlantCondition.WILTING &&
                    plant.condition != PlantCondition.WILTING
            }.map { it.id }
        )
    }

    /**
     * Applies [day] to [plants] and returns the new garden.
     *
     * [newPlantId] is only called when a seed is actually planted, so a test can
     * pass a counter and a release build uses UUIDs.
     * The caller must apply each local day once, in order; this pure function has
     * no persistence or processed-day marker and must not run on every usage sync.
     */
    fun advance(
        day: GardenDay,
        plants: List<Plant>,
        rules: GardenRules = GardenRules(),
        newPlantId: () -> String = { UUID.randomUUID().toString() }
    ): GardenOutcome {
        val bloomed = mutableListOf<String>()
        val wilted = mutableListOf<String>()
        val recovered = mutableListOf<String>()

        val grown = plants.map { plant ->
            val updated = grow(plant, day, rules)
            if (plant.stage != PlantStage.BLOOM && updated.stage == PlantStage.BLOOM) bloomed += updated.id
            if (plant.condition != PlantCondition.WILTING && updated.condition == PlantCondition.WILTING) {
                wilted += updated.id
            }
            if (plant.condition == PlantCondition.WILTING && updated.condition != PlantCondition.WILTING) {
                recovered += updated.id
            }
            updated
        }

        val planted = if (shouldPlant(day, grown, rules)) {
            val id = newPlantId()
            require(id.isNotBlank() && grown.none { it.id == id }) { "A new plant needs a unique, nonblank id" }
            newSeed(day, rules, id)
        } else {
            null
        }
        return GardenOutcome(
            plants = if (planted == null) grown else grown + planted,
            planted = planted,
            bloomed = bloomed,
            wilted = wilted,
            recovered = recovered
        )
    }

    /** Applies one day to a single plant, without the planting rule. */
    fun grow(plant: Plant, day: GardenDay, rules: GardenRules = GardenRules()): Plant {
        val previousHealth = plant.health.coerceIn(rules.minHealth, rules.maxHealth)
        val growth = (plant.growthPoints.coerceAtLeast(0).toLong() + growthFor(day, rules))
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val health = healthAfter(previousHealth, day, rules)
        val lastFocus = day.lastFocusAtUtcMillis
        return plant.copy(
            growthPoints = growth,
            health = health,
            // Nothing changed: keep the condition, or an unknown day would flip a
            // recovering plant back to wilting without any evidence.
            condition = if (health == previousHealth) plant.condition else conditionFor(previousHealth, health, rules),
            lastFocusAtUtcMillis = if (day.focusSessionsCompleted > 0 && lastFocus != null) {
                maxOf(plant.lastFocusAtUtcMillis ?: lastFocus, lastFocus)
            } else {
                plant.lastFocusAtUtcMillis
            }
        )
    }

    private fun growthFor(day: GardenDay, rules: GardenRules): Int {
        val scoreGrowth = day.score
            ?.coerceIn(0, 100)
            ?.let { (it.toDouble() * rules.growthPerFullScore / 100.0).roundToInt() }
            ?: 0
        val appliedFocusGrowth = day.focusSessionsCompleted.coerceIn(0, rules.maxCountedFocus).toLong() *
            rules.growthPerFocus
        val focusGrowth = if (day.focusRewardsAlreadyApplied) 0 else appliedFocusGrowth
        val streakGrowth = if (day.score == null) 0 else streakBonus(day.streakDays, rules)
        val remainingCap = (
            rules.maxGrowthPerDay.toLong() -
                if (day.focusRewardsAlreadyApplied) appliedFocusGrowth else 0
            ).coerceAtLeast(0)
        return (scoreGrowth.toLong() + focusGrowth + streakGrowth).coerceIn(0L, remainingCap).toInt()
    }

    private fun streakBonus(streakDays: Int, rules: GardenRules): Int {
        if (streakDays < rules.streakBonusFromDays) return 0
        return (streakDays - rules.streakBonusFromDays + 1).coerceIn(0, rules.maxStreakBonus)
    }

    private fun healthAfter(health: Int, day: GardenDay, rules: GardenRules): Int {
        val focusCount = day.focusSessionsCompleted.coerceIn(0, rules.maxCountedFocus)
        val scoreHealth = day.score
            ?.let { score ->
                when {
                    score >= rules.goodDayScore -> rules.healthGainPerGoodDay
                    score < rules.poorDayScore && focusCount == 0 -> -rules.wiltPerPoorDay
                    else -> 0
                }
            }
            ?: 0
        val focusHealth = if (day.focusRewardsAlreadyApplied) 0 else focusCount.toLong() * rules.healthPerFocus
        return (health.toLong() + scoreHealth + focusHealth)
            .coerceIn(rules.minHealth.toLong(), rules.maxHealth.toLong()).toInt()
    }

    private fun conditionFor(previousHealth: Int, health: Int, rules: GardenRules): PlantCondition = when {
        health >= rules.healthyAt -> PlantCondition.GROWING
        health > previousHealth -> PlantCondition.RECOVERING
        else -> PlantCondition.WILTING
    }

    private fun shouldPlant(day: GardenDay, plants: List<Plant>, rules: GardenRules): Boolean {
        val score = day.score ?: return false
        return score >= rules.goodDayScore && plants.size < rules.maxPlants
    }

    private fun newSeed(day: GardenDay, rules: GardenRules, id: String): Plant = Plant(
        id = id,
        species = rules.species,
        growthPoints = 0,
        health = rules.newPlantHealth,
        condition = if (rules.newPlantHealth >= rules.healthyAt) PlantCondition.GROWING else PlantCondition.RECOVERING,
        plantedAtUtcMillis = day.dayStartUtcMillis
    )
}
