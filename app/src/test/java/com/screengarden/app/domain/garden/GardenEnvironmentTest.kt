// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class GardenEnvironmentTest {
    @Test
    fun hemispheresHaveOppositeSeasonsInEveryMonth() {
        val seasons = listOf(
            Season.WINTER, Season.WINTER, Season.SPRING, Season.SPRING, Season.SPRING, Season.SUMMER,
            Season.SUMMER, Season.SUMMER, Season.AUTUMN, Season.AUTUMN, Season.AUTUMN, Season.WINTER
        )
        for (month in 1..12) {
            val moment = ZonedDateTime.parse("2026-${month.toString().padStart(2, '0')}-01T12:00:00Z")
            assertEquals(seasons[month - 1], GardenEnvironment.at(moment, Hemisphere.NORTHERN, null).season)
            assertEquals(seasons[(month + 5) % 12], GardenEnvironment.at(moment, Hemisphere.SOUTHERN, null).season)
        }
    }

    @Test
    fun clockBandsUseTheLocalZoneAndExactBoundaries() {
        val moment = ZonedDateTime.parse("2026-06-01T00:00:00+06:00[Asia/Dhaka]")
        for ((hour, light) in listOf(
            0 to GardenLight.NIGHT,
            5 to GardenLight.NIGHT,
            6 to GardenLight.DAY,
            16 to GardenLight.DAY,
            17 to GardenLight.SUNSET,
            19 to GardenLight.SUNSET,
            20 to GardenLight.NIGHT,
            23 to GardenLight.NIGHT
        )) {
            assertEquals(light, GardenEnvironment.at(moment.withHour(hour), Hemisphere.NORTHERN, 80).light)
        }
    }

    @Test
    fun weatherIsDecorativeAndUnknownScoreIsNotAGoodDay() {
        val winter = ZonedDateTime.parse("2026-01-01T12:00:00Z")
        val summer = winter.withMonth(7)
        assertEquals(GardenWeather.SNOW, GardenEnvironment.at(winter, Hemisphere.NORTHERN, 80).weather)
        assertEquals(GardenWeather.RAIN, GardenEnvironment.at(summer, Hemisphere.NORTHERN, 30).weather)
        assertEquals(GardenWeather.SUN_RAYS, GardenEnvironment.at(summer, Hemisphere.NORTHERN, null).weather)
        assertEquals(GardenWeather.CLEAR, GardenEnvironment.at(summer.withHour(22), Hemisphere.NORTHERN, null).weather)
        assertEquals(
            GardenWeather.FIREFLIES,
            GardenEnvironment.at(summer.withHour(22), Hemisphere.NORTHERN, 80).weather
        )
        assertEquals(Hemisphere.NORTHERN, Hemisphere.fromStorageKey("future"))
    }
}
