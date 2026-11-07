// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.debug

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.data.usage.AndroidUsageSource
import com.screengarden.app.domain.focus.FocusSessionState
import com.screengarden.app.domain.focus.FocusStatus
import com.screengarden.app.domain.focus.FocusTag
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.ui.debug.UsageSelfCheckScreen
import com.screengarden.app.ui.navigation.DebugDestination
import com.screengarden.app.ui.navigation.ScreenGardenDestination
import com.screengarden.app.ui.screens.focus.FocusScreen
import com.screengarden.app.ui.screens.focus.FocusUiState
import com.screengarden.app.ui.screens.garden.GardenAvailability
import com.screengarden.app.ui.screens.garden.GardenScreen
import com.screengarden.app.ui.screens.garden.GardenUiState

/**
 * The debug-build catalogue of diagnostic screens (P05b).
 *
 * This file exists only in `src/debug`, so the screen it references cannot reach a
 * release build. The release source set provides a stub with the same signature
 * that always returns null.
 *
 * Add a screen here rather than a second Activity: H5 keeps the app to one
 * Activity, and a debug destination keeps it that way (D49).
 */
object DebugCatalog {

    /** Route and intent-extra value for the usage self-check screen. */
    const val USAGE_SELF_CHECK: String = "usage_self_check"

    /** The requested debug destination, or null when [requested] names none. */
    fun destination(requested: String?, context: Context): DebugDestination? {
        if (requested in
            setOf("focus_demo", "focus_running_demo", "focus_result_demo", "focus_soft_demo", "focus_rtl_demo")
        ) {
            val fake = FocusSessionState(
                "demo-focus",
                1794000000000L,
                1794000000000L,
                25,
                FocusTag.READING,
                actualDurationMs = 720_000
            )
            val result = fake.copy(
                status = if (requested == "focus_soft_demo") {
                    FocusStatus.FAILED_SOFT
                } else {
                    FocusStatus.COMPLETED
                },
                endedAtUtcMillis = 1794001500000L,
                actualDurationMs = 1_500_000
            )
            val state = FocusUiState(
                loading = false,
                sessionsToday = 2,
                minutesThisWeek = 115,
                active = fake.takeIf { requested == "focus_running_demo" },
                result = result.takeIf { requested in setOf("focus_result_demo", "focus_soft_demo") }
            )
            return DebugDestination(requireNotNull(requested), ScreenGardenDestination.Focus) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (requested == "focus_rtl_demo") {
                        LayoutDirection.Rtl
                    } else {
                        LocalLayoutDirection.current
                    }
                ) {
                    FocusScreen(state, { _, _ -> }, { }, { }, { }, { })
                }
            }
        }
        if (requested in setOf("garden_demo", "garden_rtl_demo", "garden_empty_demo", "garden_permission_demo")) {
            val state = when (requested) {
                "garden_permission_demo" -> GardenUiState(false, GardenAvailability.NO_PERMISSION)
                "garden_empty_demo" -> GardenUiState(false, GardenAvailability.NO_DATA)
                else -> GardenUiState(
                    loading = false,
                    availability = GardenAvailability.READY,
                    plants = listOf(
                        Plant("demo-flower", "flower", 45, 90, plantedAtUtcMillis = 1793318400000L),
                        Plant("demo-bonsai", "bonsai", 27, 70, plantedAtUtcMillis = 1793491200000L),
                        Plant("demo-cactus", "cactus", 45, 82, plantedAtUtcMillis = 1793664000000L),
                        Plant("demo-fern", "fern", 15, 44, PlantCondition.RECOVERING, 1793664000000L),
                        Plant("demo-seed", "fern", plantedAtUtcMillis = 1793750400000L)
                    ),
                    score = 84,
                    streakDays = 4
                )
            }
            // Pure fake presentation: never reads or writes anybody's usage or garden.
            return DebugDestination(requireNotNull(requested)) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (requested == "garden_rtl_demo") {
                        LayoutDirection.Rtl
                    } else {
                        LocalLayoutDirection.current
                    }
                ) {
                    GardenScreen(state, { }, { }, { })
                }
            }
        }
        if (requested != USAGE_SELF_CHECK) return null
        val appContext = context.applicationContext
        val container = (appContext as ScreenGardenApplication).container
        // Built once per launch so the screen reports the same source the app uses.
        val source = AndroidUsageSource(appContext)
        return DebugDestination(USAGE_SELF_CHECK) {
            UsageSelfCheckScreen(
                usageRepository = container.usageRepository,
                source = source
            )
        }
    }
}
