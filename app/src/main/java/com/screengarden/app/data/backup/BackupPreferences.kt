// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.screengarden.app.data.settings.SettingsDefaults
import com.screengarden.app.domain.garden.Hemisphere
import com.screengarden.app.domain.model.AccentColor
import com.screengarden.app.domain.model.ThemePreference
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import org.json.JSONObject

/** Only documented user preferences; never copy platform grants or private delivery ledgers. */
internal class BackupPreferences(private val store: DataStore<Preferences>) {
    suspend fun snapshot(): JSONObject {
        val saved = store.data.first().asMap().mapKeys { it.key.name }
        return defaults().apply { keys().asSequence().toList().forEach { key -> saved[key]?.let { put(key, it) } } }
    }

    suspend fun apply(json: JSONObject) {
        validate(json)
        store.edit { target ->
            defaults().keys().forEach { key ->
                when (val value = json.get(key)) {
                    is Boolean -> target[booleanPreferencesKey(key)] = value
                    is String -> target[stringPreferencesKey(key)] = value
                    is Number -> if (key == "daily_goal_ms") {
                        target[longPreferencesKey(key)] = value.toLong()
                    } else {
                        target[intPreferencesKey(key)] = value.toInt()
                    }
                }
            }
        }
    }

    companion object {
        fun defaults(): JSONObject = JSONObject()
            .put("daily_goal_ms", SettingsDefaults.DAILY_GOAL_MS)
            .put("pickup_goal", SettingsDefaults.PICKUP_GOAL)
            .put("bedtime_start", SettingsDefaults.BEDTIME_START)
            .put("bedtime_end", SettingsDefaults.BEDTIME_END)
            .put("theme", SettingsDefaults.THEME.storageKey)
            .put("accent_color", SettingsDefaults.ACCENT_COLOR.storageKey)
            .put("streak_threshold", SettingsDefaults.STREAK_THRESHOLD)
            .put("gentle_mode", SettingsDefaults.GENTLE_MODE)
            .put("notifications_on", SettingsDefaults.NOTIFICATIONS_ON)
            .put("onboarding_done", SettingsDefaults.ONBOARDING_DONE)
            .put("keep_screen_on", SettingsDefaults.KEEP_SCREEN_ON)
            .put("compassion_days_per_month", SettingsDefaults.COMPASSION_DAYS_PER_MONTH)
            .put("hemisphere", SettingsDefaults.HEMISPHERE.name)
            .put("low_power_visuals", SettingsDefaults.LOW_POWER_VISUALS)

        fun validate(json: JSONObject) {
            val expected = defaults()
            require(json.keys().asSequence().toSet() == expected.keys().asSequence().toSet())
            expected.keys().forEach { key ->
                when (expected.get(key)) {
                    is Boolean -> require(json.get(key) is Boolean)
                    is String -> require(json.get(key) is String && json.getString(key).length <= 32)
                    is Number -> BackupCodec.integer(
                        json.get(key),
                        if (key == "daily_goal_ms") 1 else 0,
                        if (key == "daily_goal_ms") Long.MAX_VALUE else Int.MAX_VALUE.toLong()
                    )
                }
            }
            require(json.getInt("streak_threshold") in 0..100)
            require(json.getInt("compassion_days_per_month") in 0..10)
            require(json.getString("theme") in ThemePreference.entries.map { it.storageKey })
            require(json.getString("accent_color") in AccentColor.entries.map { it.storageKey })
            require(json.getString("hemisphere") in Hemisphere.entries.map { it.name })
            for (key in listOf("bedtime_start", "bedtime_end")) {
                val value = json.getString(key)
                require(Regex("[0-2][0-9]:[0-5][0-9]").matches(value))
                LocalTime.parse(value)
            }
        }
    }
}
