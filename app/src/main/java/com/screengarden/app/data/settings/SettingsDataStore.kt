// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.screengarden.app.domain.settings.SettingsRepository

/** File name of the single settings store; changing it would orphan saved settings. */
private const val SETTINGS_FILE_NAME = "screengarden_settings"

/**
 * The process-wide Preferences DataStore behind [SettingsRepository].
 *
 * This has to be a top-level property delegate: DataStore allows exactly one
 * active instance per file and throws on a second one, so creating it inside a
 * repository, a screen or a view model would crash on the second constructor.
 * The file lives in internal app storage and is excluded from backup because the
 * whole app is (H2).
 *
 * A damaged file is replaced by an empty one instead of raising through the UI
 * (DECISION D40): losing settings is annoying, but a crash loop on launch would
 * leave the user with no way to change them back.
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_FILE_NAME,
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)
