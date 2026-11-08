// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.model

/**
 * A calm palette variant selected by the user (AGENT_MEMORY SECTION 16.7
 * `accentColor`).
 *
 * Three curated palettes keep the neutral surface roles. The original storage
 * key remains stable so existing installs keep their green palette.
 * Unknown keys fall back to [CALM_GREEN] for
 * the same reason as [ThemePreference].
 */
enum class AccentColor(val storageKey: String) {
    CALM_GREEN("calm_green"),
    SOFT_BLUE("soft_blue"),
    HEATHER("heather");

    companion object {
        /** Maps a stored key back to a variant; anything unknown means [CALM_GREEN]. */
        fun fromStorageKey(storageKey: String?): AccentColor =
            entries.firstOrNull { it.storageKey == storageKey } ?: CALM_GREEN
    }
}
