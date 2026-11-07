// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

/**
 * The numbers that decide how far along a plant is and how healthy it is
 * (AGENT_MEMORY 16.2).
 *
 * Growth and health are deliberately separate: growth never goes backwards (a
 * plant does not shrink because of a bad week), while health is what wilting and
 * recovery move. That split is what makes "never permanently lost" true by
 * construction rather than by a special case.
 */
object PlantTuning {

    /** Growth points that turn a seed into a sprout. */
    const val SPROUT_AT: Int = 10

    /** Growth points that turn a sprout into a bud. */
    const val BUD_AT: Int = 25

    /** Growth points that open the bud into a bloom. */
    const val BLOOM_AT: Int = 45

    /** A plant at or above this health is growing normally. */
    const val HEALTHY_AT: Int = 60

    /**
     * The floor of health. A plant can wilt this far and no further, so there is
     * no "dead plant" to delete and any good day can start the recovery.
     */
    const val MIN_HEALTH: Int = 10

    /** The ceiling of health, matching the 0..100 health column. */
    const val MAX_HEALTH: Int = 100

    /** What a freshly planted seed starts with. */
    const val NEW_PLANT_HEALTH: Int = 60
}

/**
 * How far a plant has grown: SEED -> SPROUT -> BUD -> BLOOM.
 *
 * The stage is derived from [Plant.growthPoints] instead of being stored, so a
 * plant can never be "a bloom with seed-sized growth". WILTING and RECOVERING are
 * not stages: they are conditions ([PlantCondition]), because a wilted bloom must
 * come back as a bloom and not as a smaller plant (DECISION D58).
 */
enum class PlantStage(val minimumGrowthPoints: Int) {

    /** Nothing above the soil yet. */
    SEED(0),

    /** A first shoot. */
    SPROUT(PlantTuning.SPROUT_AT),

    /** A bud, close to opening. */
    BUD(PlantTuning.BUD_AT),

    /** Fully grown; growth saturates at Int.MAX_VALUE and the stage stops changing. */
    BLOOM(PlantTuning.BLOOM_AT);

    companion object {

        /** The stage [growthPoints] has reached; negative points count as a seed. */
        fun forGrowthPoints(growthPoints: Int): PlantStage {
            val points = growthPoints.coerceAtLeast(0)
            return entries.last { points >= it.minimumGrowthPoints }
        }
    }
}

/**
 * The condition a plant is in, as opposed to how big it is.
 *
 * WILTING is gradual (health falls a little on each poor day) and RECOVERING is
 * the period health rises below the healthy line. Both are reversible: nothing here can
 * remove a plant (16.2).
 */
enum class PlantCondition {

    /** Health is at or above [PlantTuning.HEALTHY_AT]. */
    GROWING,

    /** Health fell below the healthy line and did not rise this day. */
    WILTING,

    /** Health is below the healthy line but rose this day. */
    RECOVERING
}

/**
 * One plant, as the domain reasons about it (AGENT_MEMORY 16.2).
 *
 * [growthPoints] and [condition] are not stored yet - the plants table has stage,
 * health and timestamps (SECTION 15), and growth points are the v2 field listed in
 * K24. P09 is pure logic, so nothing is persisted by this task.
 *
 * [condition] is written by [GardenEngine] and never inferred from health alone:
 * RECOVERING means "health rose today", which no single health value can express.
 */
data class Plant(
    val id: String,
    val species: String,
    val growthPoints: Int = 0,
    val health: Int = PlantTuning.NEW_PLANT_HEALTH,
    val condition: PlantCondition = PlantCondition.GROWING,
    val plantedAtUtcMillis: Long = 0L,
    val lastFocusAtUtcMillis: Long? = null
) {

    /** The size this plant has reached, derived from [growthPoints]. */
    val stage: PlantStage get() = PlantStage.forGrowthPoints(growthPoints)

    /** True while the plant is losing health day after day. */
    val isWilting: Boolean get() = condition == PlantCondition.WILTING

    /** True while health is rising below the healthy line. */
    val isRecovering: Boolean get() = condition == PlantCondition.RECOVERING
}
