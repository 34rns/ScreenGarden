// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.screengarden.app.domain.model.AccentColor
import com.screengarden.app.domain.model.ThemePreference
import java.io.IOException
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Storage keys for [SettingsRepository].
 *
 * The names are stable identifiers, not labels: renaming one silently resets that
 * setting for every existing install, so they are only ever added, never renamed.
 */
internal object SettingsKeys {
    val DAILY_GOAL_MS = longPreferencesKey("daily_goal_ms")
    val PICKUP_GOAL = intPreferencesKey("pickup_goal")
    val BEDTIME_START = stringPreferencesKey("bedtime_start")
    val BEDTIME_END = stringPreferencesKey("bedtime_end")
    val THEME = stringPreferencesKey("theme")
    val ACCENT_COLOR = stringPreferencesKey("accent_color")
    val STREAK_THRESHOLD = intPreferencesKey("streak_threshold")
    val GENTLE_MODE = booleanPreferencesKey("gentle_mode")
    val NOTIFICATIONS_ON = booleanPreferencesKey("notifications_on")
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
}

/**
 * [SettingsRepository] on top of a Preferences [DataStore].
 *
 * Reads always fall back to [SettingsDefaults], so a fresh install and a file
 * that lost a key behave identically. A read failure caused by a corrupt file is
 * swallowed into an empty snapshot on purpose: losing settings is bad, but
 * crashing the shell of the app is worse, and the user can set them again.
 *
 * Writes validate their input here rather than in the (future) settings UI so no
 * caller can persist a value the rest of the app cannot interpret.
 */
class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    override val dailyGoalMs: Flow<Long> =
        preferences { it[SettingsKeys.DAILY_GOAL_MS] ?: SettingsDefaults.DAILY_GOAL_MS }

    override val pickupGoal: Flow<Int> =
        preferences { it[SettingsKeys.PICKUP_GOAL] ?: SettingsDefaults.PICKUP_GOAL }

    override val bedtimeStart: Flow<String> =
        preferences { it[SettingsKeys.BEDTIME_START] ?: SettingsDefaults.BEDTIME_START }

    override val bedtimeEnd: Flow<String> =
        preferences { it[SettingsKeys.BEDTIME_END] ?: SettingsDefaults.BEDTIME_END }

    override val theme: Flow<ThemePreference> = preferences {
        ThemePreference.fromStorageKey(it[SettingsKeys.THEME] ?: SettingsDefaults.THEME.storageKey)
    }

    override val accentColor: Flow<AccentColor> = preferences {
        AccentColor.fromStorageKey(it[SettingsKeys.ACCENT_COLOR] ?: SettingsDefaults.ACCENT_COLOR.storageKey)
    }

    override val streakThreshold: Flow<Int> =
        preferences { it[SettingsKeys.STREAK_THRESHOLD] ?: SettingsDefaults.STREAK_THRESHOLD }

    override val gentleMode: Flow<Boolean> =
        preferences { it[SettingsKeys.GENTLE_MODE] ?: SettingsDefaults.GENTLE_MODE }

    override val notificationsOn: Flow<Boolean> =
        preferences { it[SettingsKeys.NOTIFICATIONS_ON] ?: SettingsDefaults.NOTIFICATIONS_ON }

    override val onboardingDone: Flow<Boolean> =
        preferences { it[SettingsKeys.ONBOARDING_DONE] ?: SettingsDefaults.ONBOARDING_DONE }

    override suspend fun setDailyGoalMs(goalMs: Long) {
        require(goalMs > 0L) { "dailyGoalMs must be positive, was $goalMs" }
        dataStore.edit { it[SettingsKeys.DAILY_GOAL_MS] = goalMs }
    }

    override suspend fun setPickupGoal(goal: Int) {
        require(goal >= 0) { "pickupGoal must not be negative, was $goal" }
        dataStore.edit { it[SettingsKeys.PICKUP_GOAL] = goal }
    }

    override suspend fun setBedtimeStart(value: String) {
        val canonical = canonicalBedtime(value)
        dataStore.edit { it[SettingsKeys.BEDTIME_START] = canonical }
    }

    override suspend fun setBedtimeEnd(value: String) {
        val canonical = canonicalBedtime(value)
        dataStore.edit { it[SettingsKeys.BEDTIME_END] = canonical }
    }

    override suspend fun setTheme(preference: ThemePreference) {
        dataStore.edit { it[SettingsKeys.THEME] = preference.storageKey }
    }

    override suspend fun setAccentColor(color: AccentColor) {
        dataStore.edit { it[SettingsKeys.ACCENT_COLOR] = color.storageKey }
    }

    override suspend fun setStreakThreshold(threshold: Int) {
        require(threshold in MIN_SCORE..MAX_SCORE) {
            "streakThreshold must be within $MIN_SCORE..$MAX_SCORE, was $threshold"
        }
        dataStore.edit { it[SettingsKeys.STREAK_THRESHOLD] = threshold }
    }

    override suspend fun setGentleMode(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.GENTLE_MODE] = enabled }
    }

    override suspend fun setNotificationsOn(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.NOTIFICATIONS_ON] = enabled }
    }

    override suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[SettingsKeys.ONBOARDING_DONE] = done }
    }

    /**
     * Reads one key with the two safety nets every caller would otherwise forget:
     * an absent key reports its default, and an unreadable file reports defaults
     * instead of throwing.
     */
    private inline fun <T> preferences(crossinline read: (Preferences) -> T): Flow<T> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { read(it) }

    /** Accepts ISO local times such as "22:30" and stores them as HH:mm. */
    private fun canonicalBedtime(value: String): String = try {
        LocalTime.parse(value).format(BEDTIME_FORMAT)
    } catch (exception: DateTimeParseException) {
        throw IllegalArgumentException("Bedtime must be a local time in HH:mm form, was '$value'", exception)
    }

    private companion object {
        /** Health scores are 0..100 (SECTION 16.1). */
        const val MIN_SCORE = 0
        const val MAX_SCORE = 100

        val BEDTIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
