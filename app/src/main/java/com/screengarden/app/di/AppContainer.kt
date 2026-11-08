// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.di

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.work.WorkManager
import com.screengarden.app.R
import com.screengarden.app.core.time.BedtimeWindow
import com.screengarden.app.data.focus.FocusController
import com.screengarden.app.data.focus.FocusUseReader
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.data.notifications.AndroidNudgePublisher
import com.screengarden.app.data.notifications.GentleNudgeCheck
import com.screengarden.app.data.notifications.NudgePreferences
import com.screengarden.app.data.notifications.PreferencesNudgeLedger
import com.screengarden.app.data.onboarding.OnboardingCompletion
import com.screengarden.app.data.repo.AppLimitRepositoryImpl
import com.screengarden.app.data.repo.AppUsageRepository
import com.screengarden.app.data.repo.AppUsageRepositoryImpl
import com.screengarden.app.data.repo.BadgeRepositoryImpl
import com.screengarden.app.data.repo.DailyStatRepository
import com.screengarden.app.data.repo.DailyStatRepositoryImpl
import com.screengarden.app.data.repo.FocusSessionRepositoryImpl
import com.screengarden.app.data.repo.GardenHistoryRepository
import com.screengarden.app.data.repo.GardenRepositoryImpl
import com.screengarden.app.data.repo.HabitsRepository
import com.screengarden.app.data.repo.InsightsRepository
import com.screengarden.app.data.settings.DataStoreSettingsRepository
import com.screengarden.app.data.settings.SettingsRepository
import com.screengarden.app.data.settings.settingsDataStore
import com.screengarden.app.data.sync.DailyUsageSync
import com.screengarden.app.data.usage.AndroidUsageSource
import com.screengarden.app.data.usage.DefaultUsageRepository
import com.screengarden.app.data.usage.LaunchableAppCatalog
import com.screengarden.app.data.work.NudgeScheduler
import com.screengarden.app.data.work.UsageSyncScheduler
import com.screengarden.app.ui.screens.focus.FocusViewModel
import com.screengarden.app.ui.screens.garden.GardenViewModel
import com.screengarden.app.ui.screens.habits.HabitsViewModel
import com.screengarden.app.ui.screens.insights.GardenHistoryViewModel
import com.screengarden.app.ui.screens.insights.InsightsViewModel
import com.screengarden.app.ui.screens.limits.AppLimitsViewModel
import java.time.LocalTime
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Manual dependency injection root, created once by [com.screengarden.app.ScreenGardenApplication].
 *
 * DECISION: manual DI instead of Hilt.
 * - No kapt step (only KSP for Room, which we need anyway), so the toolchain
 *   stays simple.
 * - The app has one Activity and no dynamic feature modules, so a graph library
 *   buys nothing yet.
 * - Everything is constructed here and nowhere else, so a screen cannot quietly
 *   build its own database or DataStore.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /**
     * The Room database. Lazy so a cold start does not touch disk until a screen
     * actually asks for data.
     */
    val database: ScreenGardenDatabase by lazy { ScreenGardenDatabase.build(appContext) }

    /**
     * User settings (AGENT_MEMORY 16.7), read at the composition root to pick the
     * theme and later by the settings UI.
     */
    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(appContext.settingsDataStore)
    }

    /**
     * Persists the first-run answers (P07). Built here rather than in the Activity so
     * the flow and a future settings screen share one write path, and so the write
     * order (goals first, onboardingDone last) lives in one place.
     */
    val onboardingCompletion: OnboardingCompletion by lazy {
        OnboardingCompletion(settingsRepository)
    }

    /**
     * Screen time read straight from the OS (P05). Read-only; the daily sync (P06)
     * is what persists it into `app_usage_daily`.
     */
    val usageRepository: DefaultUsageRepository by lazy {
        DefaultUsageRepository(AndroidUsageSource(appContext), bedtimeWindow = { bedtimeWindow.first() })
    }

    /** Day aggregates; written by the daily sync (P06), read by Insights later. */
    val dailyStatRepository: DailyStatRepository by lazy {
        DailyStatRepositoryImpl(database.dailyStatDao())
    }

    val gardenRepository: GardenRepositoryImpl by lazy { GardenRepositoryImpl(database) }
    val gardenHistoryRepository: GardenHistoryRepository by lazy {
        GardenHistoryRepository(database.gardenHistoryDao())
    }
    fun gardenHistoryViewModel(): GardenHistoryViewModel = GardenHistoryViewModel(gardenHistoryRepository)
    val badgeRepository: BadgeRepositoryImpl by lazy { BadgeRepositoryImpl(database.badgeDao()) }
    val habitsRepository: HabitsRepository by lazy { HabitsRepository(database) }
    val insightsRepository: InsightsRepository by lazy {
        InsightsRepository(
            database,
            gardenRepository,
            usageRepository,
            { appCatalog.installedApps().associate { it.packageName to it.label } }
        )
    }

    fun insightsViewModel(): InsightsViewModel = InsightsViewModel(
        insightsRepository,
        settingsRepository.dailyGoalMs,
        settingsRepository.pickupGoal,
        { usageRepository.isUsageAccessGranted() },
        {
            dailyUsageSync.run()
            Unit
        }
    )

    fun habitsViewModel(): HabitsViewModel = HabitsViewModel(
        habitsRepository,
        settingsRepository.streakThreshold,
        settingsRepository.compassionDaysPerMonth,
        settingsRepository::setStreakThreshold,
        settingsRepository::setCompassionDaysPerMonth
    )

    val appLimitRepository: AppLimitRepositoryImpl by lazy { AppLimitRepositoryImpl(database.appLimitDao()) }
    private val appCatalog: LaunchableAppCatalog by lazy { LaunchableAppCatalog(appContext) }

    fun appLimitsViewModel(): AppLimitsViewModel = AppLimitsViewModel(appLimitRepository, appCatalog::installedApps)

    val focusSessionRepository: FocusSessionRepositoryImpl by lazy { FocusSessionRepositoryImpl(database) }
    val focusController: FocusController by lazy { FocusController(appContext) }
    val focusUseReader: FocusUseReader by lazy {
        // Settings exposes FallbackHome on some devices; only the active default launcher is exempt.
        val home = appContext.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName
        FocusUseReader(usageRepository, setOfNotNull(home, appContext.packageName))
    }

    fun focusViewModel(): FocusViewModel = FocusViewModel(
        store = focusSessionRepository,
        keepScreenOn = settingsRepository.keepScreenOn,
        startTimer = { minutes, tag -> focusController.start(minutes, tag) },
        stopTimer = focusController::stop,
        saveKeepScreenOn = settingsRepository::setKeepScreenOn
    )

    fun gardenViewModel(): GardenViewModel = GardenViewModel(
        store = gardenRepository,
        dailyGoalMs = settingsRepository.dailyGoalMs,
        pickupGoal = settingsRepository.pickupGoal,
        streakThreshold = settingsRepository.streakThreshold,
        hasUsageAccess = { usageRepository.isUsageAccessGranted() },
        syncUsage = {
            dailyUsageSync.run()
            Unit
        },
        bedtimeWindow = bedtimeWindow
    )

    /** Per-app screen time; written by the daily sync (P06). */
    val appUsageRepository: AppUsageRepository by lazy {
        AppUsageRepositoryImpl(database.appUsageDailyDao())
    }

    /**
     * The one object that turns a day of OS usage into Room rows (P06). Built here so
     * both the worker and the on-open sync use the same instance and the same zones.
     */
    val dailyUsageSync: DailyUsageSync by lazy {
        DailyUsageSync(
            usageRepository = usageRepository,
            dailyStatRepository = dailyStatRepository,
            appUsageRepository = appUsageRepository
        )
    }

    /** Enqueues the sync work; see [UsageSyncScheduler] for the two triggers. */
    val usageSyncScheduler: UsageSyncScheduler by lazy {
        // A provider, not an instance: WorkManager.getInstance throws when the library
        // is not initialised yet (direct boot, tests), and that must not crash a launch.
        UsageSyncScheduler { WorkManager.getInstance(appContext) }
    }

    val nudgeScheduler: NudgeScheduler by lazy { NudgeScheduler { WorkManager.getInstance(appContext) } }

    private val bedtimeWindow by lazy {
        combine(settingsRepository.bedtimeStart, settingsRepository.bedtimeEnd) { start, end ->
            runCatching { BedtimeWindow(LocalTime.parse(start), LocalTime.parse(end)) }.getOrNull()
        }
    }

    val nudgeCheck: GentleNudgeCheck by lazy {
        GentleNudgeCheck(
            preferences = {
                combine(
                    settingsRepository.notificationsOn,
                    settingsRepository.dailyGoalMs,
                    settingsRepository.bedtimeStart,
                    settingsRepository.bedtimeEnd
                ) { enabled, goal, start, end ->
                    NudgePreferences(
                        enabled,
                        goal,
                        runCatching {
                            BedtimeWindow(LocalTime.parse(start), LocalTime.parse(end))
                        }.getOrNull()
                    )
                }.first()
            },
            readUsage = { usageRepository.readDay(it) },
            readLimits = { appLimitRepository.observeLimits().first() },
            ledger = PreferencesNudgeLedger(appContext),
            publisher = AndroidNudgePublisher(appContext) { name ->
                appCatalog.installedApps().firstOrNull { it.packageName == name }?.label
                    ?: appContext.getString(R.string.nudge_unknown_app)
            }
        )
    }
}
