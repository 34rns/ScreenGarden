// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure JVM tests: theme resolution must not need Android or Compose. */
class ThemePreferenceTest {

    @Test
    fun systemFollowsTheDeviceSetting() {
        assertTrue(ThemePreference.SYSTEM.prefersDark(systemInDarkTheme = true))
        assertFalse(ThemePreference.SYSTEM.prefersDark(systemInDarkTheme = false))
    }

    @Test
    fun explicitLightIgnoresTheDeviceSetting() {
        assertFalse(ThemePreference.LIGHT.prefersDark(systemInDarkTheme = true))
    }

    @Test
    fun explicitDarkIgnoresTheDeviceSetting() {
        assertTrue(ThemePreference.DARK.prefersDark(systemInDarkTheme = false))
    }

    @Test
    fun amoledIsAlwaysDarkAndFlagged() {
        assertTrue(ThemePreference.AMOLED.prefersDark(systemInDarkTheme = false))
        assertTrue(ThemePreference.AMOLED.isAmoled)
        assertFalse(ThemePreference.DARK.isAmoled)
    }

    @Test
    fun storageKeysRoundTrip() {
        ThemePreference.entries.forEach { preference ->
            assertEquals(preference, ThemePreference.fromStorageKey(preference.storageKey))
        }
    }

    @Test
    fun unknownOrMissingKeysFallBackToSystem() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStorageKey("sepia"))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStorageKey(null))
    }

    @Test
    fun unknownAccentFallsBackToCalmGreen() {
        assertEquals(AccentColor.CALM_GREEN, AccentColor.fromStorageKey("neon_pink"))
        assertEquals(AccentColor.CALM_GREEN, AccentColor.fromStorageKey(null))
    }
}
