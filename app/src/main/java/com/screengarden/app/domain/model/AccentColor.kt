// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.model

/**
 * A calm palette variant selected by the user (AGENT_MEMORY SECTION 16.7
 * `accentColor`).
 *
 * Only one variant exists today: the palette in ui/theme/Color.kt. The setting is
 * stored now so that adding a second palette later is a data-only change and the
 * already-persisted choice survives. Unknown keys fall back to [CALM_GREEN] for
 * the same reason as [ThemePreference].
 */
enum class AccentColor(val storageKey: String) {
    CALM_GREEN("calm_green");

    companion object {
        /** Maps a stored key back to a variant; anything unknown means [CALM_GREEN]. */
        fun fromStorageKey(storageKey: String?): AccentColor =
            entries.firstOrNull { it.storageKey == storageKey } ?: CALM_GREEN
    }
}
