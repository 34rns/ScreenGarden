// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Calm green palette. Every role used by the Material 3 schemes is declared here
 * as a token so light, dark and AMOLED stay in sync and are easy to tune.
 */

// ---- Light ----
internal val LightPrimary = Color(0xFF3F6B4E)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFC1E8CB)
internal val LightOnPrimaryContainer = Color(0xFF00210F)

internal val LightSecondary = Color(0xFF506353)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFD3E8D5)
internal val LightOnSecondaryContainer = Color(0xFF0E1F13)

internal val LightTertiary = Color(0xFF3B6470)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFBFE9F7)
internal val LightOnTertiaryContainer = Color(0xFF001F27)

internal val LightError = Color(0xFFBA1A1A)
internal val LightOnError = Color(0xFFFFFFFF)
internal val LightErrorContainer = Color(0xFFFFDAD6)
internal val LightOnErrorContainer = Color(0xFF410002)

internal val LightBackground = Color(0xFFF6FBF4)
internal val LightOnBackground = Color(0xFF181D18)
internal val LightSurface = Color(0xFFF6FBF4)
internal val LightOnSurface = Color(0xFF181D18)
internal val LightSurfaceVariant = Color(0xFFDCE5DA)
internal val LightOnSurfaceVariant = Color(0xFF404942)
internal val LightOutline = Color(0xFF707970)
internal val LightOutlineVariant = Color(0xFFC0C9BF)

internal val LightSurfaceDim = Color(0xFFD6DBD4)
internal val LightSurfaceBright = Color(0xFFF6FBF4)
internal val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
internal val LightSurfaceContainerLow = Color(0xFFF0F5EE)
internal val LightSurfaceContainer = Color(0xFFEAEFE8)
internal val LightSurfaceContainerHigh = Color(0xFFE5EAE2)
internal val LightSurfaceContainerHighest = Color(0xFFDFE4DD)

internal val LightInverseSurface = Color(0xFF2D322D)
internal val LightInverseOnSurface = Color(0xFFEEF1EA)
internal val LightInversePrimary = Color(0xFFA5D0AF)

// ---- Dark ----
internal val DarkPrimary = Color(0xFFA5D0AF)
internal val DarkOnPrimary = Color(0xFF0A3818)
internal val DarkPrimaryContainer = Color(0xFF265238)
internal val DarkOnPrimaryContainer = Color(0xFFC1E8CB)

internal val DarkSecondary = Color(0xFFB7CCB9)
internal val DarkOnSecondary = Color(0xFF233427)
internal val DarkSecondaryContainer = Color(0xFF394B3C)
internal val DarkOnSecondaryContainer = Color(0xFFD3E8D5)

internal val DarkTertiary = Color(0xFFA3CDDB)
internal val DarkOnTertiary = Color(0xFF063541)
internal val DarkTertiaryContainer = Color(0xFF224C58)
internal val DarkOnTertiaryContainer = Color(0xFFBFE9F7)

internal val DarkError = Color(0xFFFFB4AB)
internal val DarkOnError = Color(0xFF690005)
internal val DarkErrorContainer = Color(0xFF93000A)
internal val DarkOnErrorContainer = Color(0xFFFFDAD6)

internal val DarkBackground = Color(0xFF101410)
internal val DarkOnBackground = Color(0xFFE0E4DD)
internal val DarkSurface = Color(0xFF101410)
internal val DarkOnSurface = Color(0xFFE0E4DD)
internal val DarkSurfaceVariant = Color(0xFF404942)
internal val DarkOnSurfaceVariant = Color(0xFFC0C9BF)
internal val DarkOutline = Color(0xFF8A938A)
internal val DarkOutlineVariant = Color(0xFF404942)

internal val DarkSurfaceDim = Color(0xFF101410)
internal val DarkSurfaceBright = Color(0xFF353A35)
internal val DarkSurfaceContainerLowest = Color(0xFF0B0E0B)
internal val DarkSurfaceContainerLow = Color(0xFF181D18)
internal val DarkSurfaceContainer = Color(0xFF1C211C)
internal val DarkSurfaceContainerHigh = Color(0xFF262B26)
internal val DarkSurfaceContainerHighest = Color(0xFF313631)

internal val DarkInverseSurface = Color(0xFFE0E4DD)
internal val DarkInverseOnSurface = Color(0xFF2D322D)
internal val DarkInversePrimary = Color(0xFF3F6B4E)

// ---- AMOLED ----
// Same calm greens as dark, but every surface role collapses to true black so the
// OLED panel only lights pixels that carry content. Contrast stays AA or better.
internal val AmoledSurfaceLowest = Color(0xFF000000)
internal val AmoledSurfaceLow = Color(0xFF060906)
internal val AmoledSurface = Color(0xFF000000)
internal val AmoledSurfaceContainer = Color(0xFF0C100D)
internal val AmoledSurfaceContainerHigh = Color(0xFF141814)
internal val AmoledSurfaceContainerHighest = Color(0xFF1A1F1B)
internal val AmoledOnSurfaceVariant = Color(0xFFC6CFC5)
internal val AmoledOutline = Color(0xFF8F988E)
internal val AmoledOutlineVariant = Color(0xFF2C332D)
