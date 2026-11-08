// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

import java.time.ZonedDateTime

enum class Hemisphere {
    NORTHERN,
    SOUTHERN;

    companion object {
        fun fromStorageKey(value: String): Hemisphere = entries.firstOrNull { it.name == value } ?: NORTHERN
    }
}

enum class Season { SPRING, SUMMER, AUTUMN, WINTER }
enum class GardenLight { DAY, SUNSET, NIGHT }
enum class GardenWeather { SUN_RAYS, RAIN, SNOW, FIREFLIES, CLEAR }

/** Decorative weather, never a weather report or an input to plant rewards. */
data class GardenEnvironment(
    val light: GardenLight = GardenLight.DAY,
    val season: Season = Season.SPRING,
    val weather: GardenWeather = GardenWeather.SUN_RAYS
) {
    companion object {
        /** Fixed local clock bands, not astronomical sunrise/sunset estimates. */
        fun at(moment: ZonedDateTime, hemisphere: Hemisphere, score: Int?): GardenEnvironment {
            val month = if (hemisphere == Hemisphere.NORTHERN) moment.monthValue else (moment.monthValue + 5) % 12 + 1
            val season = when (month) {
                in 3..5 -> Season.SPRING
                in 6..8 -> Season.SUMMER
                in 9..11 -> Season.AUTUMN
                else -> Season.WINTER
            }
            val light = when (moment.hour) {
                in 6..16 -> GardenLight.DAY
                in 17..19 -> GardenLight.SUNSET
                else -> GardenLight.NIGHT
            }
            val weather = when {
                light == GardenLight.NIGHT -> if (score != null &&
                    score >= 60
                ) {
                    GardenWeather.FIREFLIES
                } else {
                    GardenWeather.CLEAR
                }
                season == Season.WINTER -> GardenWeather.SNOW
                score != null && score < 60 -> GardenWeather.RAIN
                else -> GardenWeather.SUN_RAYS
            }
            return GardenEnvironment(light, season, weather)
        }
    }
}
