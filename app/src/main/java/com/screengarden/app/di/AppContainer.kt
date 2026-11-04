// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.di

import android.content.Context
import androidx.work.WorkManager
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.data.onboarding.OnboardingCompletion
import com.screengarden.app.data.repo.AppUsageRepository
import com.screengarden.app.data.repo.AppUsageRepositoryImpl
import com.screengarden.app.data.repo.DailyStatRepository
import com.screengarden.app.data.repo.DailyStatRepositoryImpl
import com.screengarden.app.data.settings.DataStoreSettingsRepository
import com.screengarden.app.data.settings.SettingsRepository
import com.screengarden.app.data.settings.settingsDataStore
import com.screengarden.app.data.sync.DailyUsageSync
import com.screengarden.app.data.usage.AndroidUsageSource
import com.screengarden.app.data.usage.DefaultUsageRepository
import com.screengarden.app.data.usage.UsageRepository
import com.screengarden.app.data.work.UsageSyncScheduler

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
    val usageRepository: UsageRepository by lazy {
        DefaultUsageRepository(AndroidUsageSource(appContext))
    }

    /** Day aggregates; written by the daily sync (P06), read by Insights later. */
    val dailyStatRepository: DailyStatRepository by lazy {
        DailyStatRepositoryImpl(database.dailyStatDao())
    }

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
}
