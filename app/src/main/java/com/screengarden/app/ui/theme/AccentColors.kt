// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.screengarden.app.domain.model.AccentColor

private val BlueLight = Color(0xFF3A607B)
private val BlueDark = Color(0xFFA2CBE8)
private val BlueLightContainer = Color(0xFFD1E5FA)
private val BlueDarkContainer = Color(0xFF224B66)
private val BlueLightInk = Color(0xFF062039)
private val BlueDarkInk = Color(0xFF07344D)
private val HeatherLight = Color(0xFF6B5276)
private val HeatherDark = Color(0xFFD7B9E2)
private val HeatherLightContainer = Color(0xFFEDDDF5)
private val HeatherDarkContainer = Color(0xFF563F61)
private val HeatherLightInk = Color(0xFF25112F)
private val HeatherDarkInk = Color(0xFF3E2849)

/** Curated accent roles keep the existing neutral, secondary and error colors. */
internal fun ColorScheme.withAccent(accent: AccentColor, dark: Boolean): ColorScheme = when (accent) {
    AccentColor.CALM_GREEN -> this
    AccentColor.SOFT_BLUE -> copy(
        primary = if (dark) BlueDark else BlueLight,
        onPrimary = if (dark) BlueDarkInk else Color.White,
        primaryContainer = if (dark) BlueDarkContainer else BlueLightContainer,
        onPrimaryContainer = if (dark) BlueLightContainer else BlueLightInk,
        inversePrimary = if (dark) BlueLight else BlueDark
    )
    AccentColor.HEATHER -> copy(
        primary = if (dark) HeatherDark else HeatherLight,
        onPrimary = if (dark) HeatherDarkInk else Color.White,
        primaryContainer = if (dark) HeatherDarkContainer else HeatherLightContainer,
        onPrimaryContainer = if (dark) HeatherLightContainer else HeatherLightInk,
        inversePrimary = if (dark) HeatherLight else HeatherDark
    )
}
