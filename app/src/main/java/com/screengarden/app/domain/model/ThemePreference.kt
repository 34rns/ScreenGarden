// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.model

/**
 * The user's persisted choice of colour scheme (AGENT_MEMORY SECTION 16.7 `theme`).
 *
 * The value is stored under [storageKey], not the enum name, so renaming a
 * constant can never silently change what an already-installed app reads back.
 * An unknown key falls back to [SYSTEM] instead of throwing: a downgrade, a
 * half-written file or a hand-edited preference must never stop the app from
 * drawing a theme.
 */
enum class ThemePreference(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    AMOLED("amoled");

    /**
     * Whether this preference asks for the dark scheme.
     *
     * [SYSTEM] is the only value that depends on the device setting; [AMOLED] is
     * always dark because it is the true-black variant of the dark palette (D6).
     */
    fun prefersDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
        AMOLED -> true
    }

    /** True only for the true-black variant, which replaces the dark surfaces. */
    val isAmoled: Boolean get() = this == AMOLED

    companion object {
        /** Maps a stored key back to a preference; anything unknown means [SYSTEM]. */
        fun fromStorageKey(storageKey: String?): ThemePreference =
            entries.firstOrNull { it.storageKey == storageKey } ?: SYSTEM
    }
}
