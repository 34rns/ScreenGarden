// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.debug

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.screengarden.app.R
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.data.notifications.AndroidNudgePublisher
import com.screengarden.app.data.usage.AndroidUsageSource
import com.screengarden.app.domain.focus.FocusSessionState
import com.screengarden.app.domain.focus.FocusStatus
import com.screengarden.app.domain.focus.FocusTag
import com.screengarden.app.domain.garden.GardenDayRecord
import com.screengarden.app.domain.garden.GardenEnvironment
import com.screengarden.app.domain.garden.GardenLight
import com.screengarden.app.domain.garden.GardenWeather
import com.screengarden.app.domain.garden.Hemisphere
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.domain.garden.Season
import com.screengarden.app.domain.habits.BadgeKind
import com.screengarden.app.domain.habits.EarnedBadge
import com.screengarden.app.domain.habits.HabitsSnapshot
import com.screengarden.app.domain.habits.Streaks
import com.screengarden.app.domain.insights.AppTime
import com.screengarden.app.domain.insights.GardenHistory
import com.screengarden.app.domain.insights.GardenHistoryFrame
import com.screengarden.app.domain.insights.HistoryPeriod
import com.screengarden.app.domain.insights.HourlyDay
import com.screengarden.app.domain.insights.InsightsPeriod
import com.screengarden.app.domain.insights.InsightsSnapshot
import com.screengarden.app.domain.insights.InsightsSummary
import com.screengarden.app.domain.insights.Reflections
import com.screengarden.app.domain.limits.AppLimitRule
import com.screengarden.app.domain.limits.LaunchableApp
import com.screengarden.app.domain.notifications.Nudge
import com.screengarden.app.ui.debug.NudgeDemoScreen
import com.screengarden.app.ui.debug.UsageSelfCheckScreen
import com.screengarden.app.ui.navigation.DebugDestination
import com.screengarden.app.ui.navigation.ScreenGardenDestination
import com.screengarden.app.ui.screens.focus.FocusScreen
import com.screengarden.app.ui.screens.focus.FocusUiState
import com.screengarden.app.ui.screens.garden.GardenAvailability
import com.screengarden.app.ui.screens.garden.GardenScreen
import com.screengarden.app.ui.screens.garden.GardenUiState
import com.screengarden.app.ui.screens.habits.HabitsScreen
import com.screengarden.app.ui.screens.habits.HabitsUiState
import com.screengarden.app.ui.screens.insights.GardenHistoryScreen
import com.screengarden.app.ui.screens.insights.GardenHistoryUiState
import com.screengarden.app.ui.screens.insights.InsightsScreen
import com.screengarden.app.ui.screens.insights.InsightsUiState
import com.screengarden.app.ui.screens.limits.AppLimitsScreen
import com.screengarden.app.ui.screens.limits.AppLimitsUiState
import com.screengarden.app.ui.screens.settings.EnvironmentControls
import com.screengarden.app.ui.screens.settings.SettingsScreen
import java.time.LocalDate
import java.time.ZoneId

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
        if (requested in setOf("history_demo", "history_months_demo", "history_empty_demo", "history_rtl_demo")) {
            val zone = ZoneId.systemDefault()
            val dates = listOf("2026-09-25", "2026-10-12", "2026-10-19", "2026-10-27", "2026-11-02", "2026-11-07")
            val frames = if (requested == "history_empty_demo") {
                emptyList()
            } else {
                dates.mapIndexed { index, date ->
                    val day = LocalDay.startOfDay(LocalDate.parse(date), zone)
                    GardenHistoryFrame(
                        day,
                        day + 86_400_000L,
                        80 + index,
                        listOf(
                            Plant("demo-flower", "flower", 10 + index * 9, 80, plantedAtUtcMillis = day),
                            Plant("demo-bonsai", "bonsai", index * 9, 72, plantedAtUtcMillis = day),
                            Plant("demo-cactus", "cactus", index * 8, 65, plantedAtUtcMillis = day)
                        )
                    )
                }
            }
            return DebugDestination(requireNotNull(requested), ScreenGardenDestination.Insights) {
                var period by remember {
                    mutableStateOf(
                        if (requested == "history_months_demo") {
                            HistoryPeriod.MONTH
                        } else {
                            HistoryPeriod.WEEK
                        }
                    )
                }
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (requested == "history_rtl_demo") {
                        LayoutDirection.Rtl
                    } else {
                        LocalLayoutDirection.current
                    }
                ) {
                    GardenHistoryScreen(
                        GardenHistoryUiState(
                            loading = false,
                            period = period,
                            entries = GardenHistory.group(frames, period, zone)
                        ),
                        { period = it },
                        {},
                        {}
                    )
                }
            }
        }
        if (requested == "environment_settings_demo") {
            return DebugDestination(requested, ScreenGardenDestination.Settings) {
                var side by remember { mutableStateOf(Hemisphere.NORTHERN) }
                var lowPower by remember { mutableStateOf(false) }
                SettingsScreen({ }, environmentContent = {
                    EnvironmentControls(side, lowPower, { side = it }, { lowPower = it })
                })
            }
        }
        if (requested in setOf(
                "insights_demo",
                "insights_empty_demo",
                "insights_permission_demo",
                "insights_rtl_demo",
                "insights_day_demo",
                "insights_month_demo"
            )
        ) {
            val today = LocalDate.of(2026, 11, 8)
            val zone = ZoneId.systemDefault()
            val days = if (requested == "insights_empty_demo") {
                emptyList()
            } else {
                (0..29).map { index ->
                    val key = LocalDay.startOfDay(today.minusDays((29 - index).toLong()), zone)
                    GardenDayRecord(
                        key, 70 + index % 4 * 6, 100 + index * 19 % 170, 25, 1, 30 + index % 10,
                        25, true, index < 29, key, isCompassionDay = index == 26
                    )
                }
            }
            val apps = days.flatMap { day ->
                val reader = day.screenMinutes * 60_000L * 3 / 5
                listOf(
                    AppTime(day.dayStartUtcMillis, "demo.reader", "Reading", reader),
                    AppTime(day.dayStartUtcMillis, "demo.notes", "Notes", day.screenMinutes * 60_000L - reader)
                )
            }
            val hours = days.takeLast(7).map { day ->
                val values = MutableList(24) { 0L }
                val total = day.screenMinutes * 60_000L
                values[8] = total / 6
                values[12] = total / 6
                values[18] = total / 5
                values[19] = total / 5
                values[20] = total / 6
                values[21] = total - values.sum()
                check(values.sum() == total && values.all { it in 0L..3_600_000L })
                HourlyDay(day.dayStartUtcMillis, values)
            }
            val snapshot = InsightsSnapshot(days, apps)
            return DebugDestination(requireNotNull(requested), ScreenGardenDestination.Insights) {
                var period by remember {
                    mutableStateOf(
                        when (requested) {
                            "insights_day_demo" -> InsightsPeriod.DAY
                            "insights_month_demo" -> InsightsPeriod.MONTH
                            else -> InsightsPeriod.WEEK
                        }
                    )
                }
                CompositionLocalProvider(
                    LocalLayoutDirection provides
                        if (requested == "insights_rtl_demo") LayoutDirection.Rtl else LocalLayoutDirection.current
                ) {
                    InsightsScreen(
                        InsightsUiState(
                            loading = false,
                            period = period,
                            hasUsageAccess = requested != "insights_permission_demo",
                            summary = InsightsSummary.build(snapshot, period, today, 180, 80, zone),
                            hours = hours,
                            reflection = Reflections.build(snapshot, today, 180, 80, zone)
                        ),
                        { period = it },
                        {},
                        {}
                    )
                }
            }
        }
        if (requested in setOf("habits_demo", "habits_empty_demo", "habits_rtl_demo")) {
            val state = HabitsUiState(
                loading = false,
                today = LocalDate.of(2026, 11, 8),
                streaks = if (requested == "habits_empty_demo") Streaks(0, 0) else Streaks(12, 30),
                snapshot = HabitsSnapshot(
                    emptyList(),
                    emptyList(),
                    if (requested ==
                        "habits_empty_demo"
                    ) {
                        emptyList()
                    } else {
                        listOf(
                            BadgeKind.STREAK_7,
                            BadgeKind.STREAK_30,
                            BadgeKind.FIRST_BLOOM,
                            BadgeKind.FIRST_RECOVERY
                        ).map {
                            EarnedBadge(it, 1794000000000L)
                        }
                    }
                )
            )
            return DebugDestination(requireNotNull(requested), ScreenGardenDestination.Settings) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (requested == "habits_rtl_demo") {
                        LayoutDirection.Rtl
                    } else {
                        LocalLayoutDirection.current
                    }
                ) {
                    HabitsScreen(state, { _, _ -> }, { }, { }, { })
                }
            }
        }
        if (requested == "nudges_demo") {
            val publisher = AndroidNudgePublisher(context) { context.getString(R.string.debug_nudge_app) }
            return DebugDestination(requested, ScreenGardenDestination.Settings) {
                NudgeDemoScreen({
                    val daily = publisher.post(Nudge(50))
                    val app = publisher.post(Nudge(80, "demo.reader"))
                    daily && app
                })
            }
        }
        if (requested == "settings_demo") {
            return DebugDestination(requested, ScreenGardenDestination.Settings) { SettingsScreen({ }) }
        }
        if (requested in setOf("limits_demo", "limits_empty_demo", "limits_rtl_demo")) {
            val state = AppLimitsUiState(
                apps = if (requested == "limits_empty_demo") {
                    emptyList()
                } else {
                    listOf(
                        LaunchableApp("demo.reader", "Reading"),
                        LaunchableApp("demo.notes", "Notes"),
                        LaunchableApp("demo.video", "Videos"),
                        LaunchableApp("demo.games", "Games")
                    )
                },
                limits = listOf(AppLimitRule("demo.video", 30), AppLimitRule("demo.games", 45, false)),
                loading = false
            )
            return DebugDestination(requireNotNull(requested), ScreenGardenDestination.Settings) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (requested == "limits_rtl_demo") {
                        LayoutDirection.Rtl
                    } else {
                        LocalLayoutDirection.current
                    }
                ) {
                    AppLimitsScreen(state, { _, _, _ -> }, { }, { })
                }
            }
        }
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
        if (requested?.startsWith("environment_") == true ||
            requested in setOf(
                "garden_demo",
                "garden_rtl_demo",
                "garden_empty_demo",
                "garden_permission_demo",
                "bedtime_demo",
                "bedtime_rtl_demo",
                "bedtime_empty_demo",
                "bedtime_permission_demo"
            )
        ) {
            val state = when (requested) {
                "garden_permission_demo", "bedtime_permission_demo" -> GardenUiState(
                    false,
                    GardenAvailability.NO_PERMISSION
                )
                "garden_empty_demo", "bedtime_empty_demo" -> GardenUiState(false, GardenAvailability.NO_DATA)
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
            }.let {
                val environment = when (requested) {
                    "environment_summer_demo" -> GardenEnvironment(season = Season.SUMMER)
                    "environment_autumn_demo" -> GardenEnvironment(season = Season.AUTUMN, weather = GardenWeather.RAIN)
                    "environment_winter_demo" -> GardenEnvironment(season = Season.WINTER, weather = GardenWeather.SNOW)
                    "environment_sunset_demo" -> GardenEnvironment(GardenLight.SUNSET, Season.AUTUMN)
                    "environment_night_demo" -> GardenEnvironment(
                        GardenLight.NIGHT,
                        Season.SUMMER,
                        GardenWeather.FIREFLIES
                    )
                    else -> GardenEnvironment()
                }
                it.copy(
                    isBedtime = requested?.startsWith("bedtime") == true,
                    environment = environment,
                    score = if (requested == "environment_autumn_demo") 42 else it.score,
                    lowPowerVisuals = requested == "environment_low_power_demo"
                )
            }
            // Pure fake presentation: never reads or writes anybody's usage or garden.
            return DebugDestination(requireNotNull(requested)) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (requested in setOf("garden_rtl_demo", "bedtime_rtl_demo")) {
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
