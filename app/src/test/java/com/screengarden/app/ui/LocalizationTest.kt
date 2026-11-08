// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.R
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.model.OnboardingChoices
import com.screengarden.app.ui.onboarding.GoalSetup
import com.screengarden.app.ui.screens.garden.GardenAvailability
import com.screengarden.app.ui.screens.garden.GardenScreen
import com.screengarden.app.ui.screens.garden.GardenUiState
import com.screengarden.app.ui.theme.ScreenGardenTheme
import java.time.Instant
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "es-w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocalizationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun mixedDurationKeepsItsReadingOrderInMirroredLayout() {
        compose.setContent {
            ScreenGardenTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    GoalSetup(OnboardingChoices(), {})
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("3 h 0 min")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(TextDirection.Content, layouts.single().layoutInput.style.textDirection)
        assertEquals(ResolvedTextDirection.Ltr, layouts.single().getParagraphDirection(0))
    }

    @Test fun spanishGardenAndDetailsUseTranslatedLabelsAndConfigurationDate() {
        val plant = Plant("sample", "fern", plantedAtUtcMillis = Instant.parse("2026-01-15T12:00:00Z").toEpochMilli())
        compose.setContent {
            ScreenGardenTheme {
                GardenScreen(GardenUiState(false, GardenAvailability.READY, listOf(plant), 72, 1), {}, {}, {})
            }
        }
        compose.onNodeWithText("Tu jardín").assertIsDisplayed()
        compose.onNodeWithTag("garden_plant_sample").performScrollTo().performClick()
        compose.onNodeWithText("Detalles de la planta").assertIsDisplayed()
        compose.onNodeWithText("15 ene 2026").assertIsDisplayed()
        compose.onNodeWithText("Cerrar detalles").performScrollTo().performClick()
    }

    @Test fun translationsPreserveFormatArgumentsAndSpanishPlurals() {
        val context: Context = ApplicationProvider.getApplicationContext()
        fun localized(language: String): Context = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        )
        val english = localized("en").resources
        val spanish = localized("es").resources
        val arguments = Regex("%[0-9]+\\\$[0-9]*[ds]")
        R.string::class.java.fields.forEach { field ->
            val id = field.getInt(null)
            assertEquals(
                field.name,
                arguments.findAll(english.getString(id)).map { it.value }.toList().sorted(),
                arguments.findAll(spanish.getString(id)).map { it.value }.toList().sorted()
            )
        }
        assertEquals("Racha de 1 día", spanish.getQuantityString(R.plurals.garden_streak_days, 1, 1))
        assertEquals("Racha de 2 días", spanish.getQuantityString(R.plurals.garden_streak_days, 2, 2))
        assertEquals("Your garden", localized("ar").getString(R.string.garden_title))
    }
}
