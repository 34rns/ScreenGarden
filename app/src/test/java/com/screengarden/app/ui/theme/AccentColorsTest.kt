// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.screengarden.app.domain.model.AccentColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentColorsTest {
    @Test fun curatedAccentsKeepAccessibleInkInLightAndDarkModes() {
        AccentColor.entries.forEach { accent ->
            listOf(false, true).forEach { dark ->
                val base = if (dark) darkColorScheme() else lightColorScheme()
                val scheme = base.withAccent(accent, dark)
                assertTrue(contrast(scheme.primary, scheme.onPrimary) >= 4.5)
                assertTrue(contrast(scheme.primaryContainer, scheme.onPrimaryContainer) >= 4.5)
            }
        }
    }

    @Test fun accentsNeverChangeTrueBlackSurfacesOrErrorRoles() {
        val amoled = darkColorScheme(background = Color.Black, surface = Color.Black)
        AccentColor.entries.forEach { accent ->
            val scheme = amoled.withAccent(accent, true)
            assertEquals(Color.Black, scheme.background)
            assertEquals(Color.Black, scheme.surface)
            assertEquals(amoled.error, scheme.error)
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val first = a.luminance().toDouble()
        val second = b.luminance().toDouble()
        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }
}
