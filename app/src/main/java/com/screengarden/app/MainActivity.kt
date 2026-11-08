// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.screengarden.app.data.settings.SettingsDefaults
import com.screengarden.app.data.usage.UsageAccess
import com.screengarden.app.debug.DebugCatalog
import com.screengarden.app.domain.model.OnboardingChoices
import com.screengarden.app.domain.model.ThemePreference
import com.screengarden.app.ui.navigation.ScreenGardenApp
import com.screengarden.app.ui.onboarding.OnboardingScreen
import com.screengarden.app.ui.screens.focus.FocusScreen
import com.screengarden.app.ui.screens.focus.FocusViewModel
import com.screengarden.app.ui.screens.garden.GardenScreen
import com.screengarden.app.ui.screens.garden.GardenViewModel
import com.screengarden.app.ui.screens.limits.AppLimitsScreen
import com.screengarden.app.ui.screens.limits.AppLimitsViewModel
import com.screengarden.app.ui.theme.ScreenGardenTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The app's only Activity. All UI is Compose; navigation lives in
 * [com.screengarden.app.ui.navigation].
 *
 * The Activity is also the composition root: it reads the settings that decide what
 * the app shows before any screen exists - the theme, and whether the first-run flow
 * still has to run (D42, D53). Screens themselves still never touch the data layer
 * (SECTION 4 layer rules); they receive values and callbacks.
 *
 * Debug-only extras are read here too, but only in a debuggable build, so a release
 * APK cannot be steered into a diagnostic screen (D49).
 *
 * Opening the app also asks for a usage sync (P06). It is deliberately done here and
 * not in the Application: a sync enqueued from every process start - including the
 * one WorkManager creates to run the worker - would keep re-queueing itself.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as ScreenGardenApplication).container
        val settingsRepository = container.settingsRepository
        val onboardingCompletion = container.onboardingCompletion
        val appContext = applicationContext
        val amoledPreview = isDebugExtraAllowed() && intent.getBooleanExtra(EXTRA_AMOLED, false)
        // DebugCatalog.destination returns null in release builds (the release
        // source set has a stub), so this is a no-op there.
        val debugDestination = requestedDebugScreen()?.let { DebugCatalog.destination(it, this) }
        // Fire-and-forget: the worker reports through logcat, and the UI shows stored
        // data, so a slow sync must not delay the first frame.
        container.usageSyncScheduler.syncNow()
        container.nudgeScheduler.checkNow()
        setContent {
            // SYSTEM is only the initial value: it covers the few milliseconds
            // before DataStore emits the stored choice (K31).
            val themePreference by settingsRepository.theme
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
            ScreenGardenTheme(
                themePreference = themePreference,
                amoled = amoledPreview
            ) {
                // null means DataStore has not answered yet. Drawing nothing for that
                // single frame is deliberate: starting at `false` would flash the
                // first-run flow at every returning user (D53).
                // The map is hoisted into remember: a Flow operator inside composition
                // builds a new Flow on every recomposition and restarts the collector.
                val onboardingDoneFlow = remember(settingsRepository) {
                    settingsRepository.onboardingDone.map<Boolean, Boolean?> { done -> done }
                }
                val onboardingDone by onboardingDoneFlow.collectAsStateWithLifecycle(initialValue = null)
                val gardenContent: @androidx.compose.runtime.Composable (() -> Unit) -> Unit = { startFocus ->
                    val factory = remember(container) {
                        viewModelFactory { initializer { container.gardenViewModel() } }
                    }
                    val garden: GardenViewModel = viewModel(factory = factory)
                    val state by garden.uiState.collectAsStateWithLifecycle()
                    val gardenLifecycle = LocalLifecycleOwner.current.lifecycle
                    LaunchedEffect(garden, gardenLifecycle) {
                        gardenLifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            while (true) {
                                garden.updateTime()
                                delay(60_000)
                            }
                        }
                    }
                    LifecycleResumeEffect(garden) {
                        garden.refresh()
                        onPauseOrDispose { }
                    }
                    GardenScreen(
                        state = state,
                        onStartFocus = startFocus,
                        onGrantUsage = { startActivity(UsageAccess.settingsIntent()) },
                        onRetry = garden::refresh
                    )
                }
                val focusContent: @androidx.compose.runtime.Composable () -> Unit = {
                    val factory = remember(container) {
                        viewModelFactory { initializer { container.focusViewModel() } }
                    }
                    val focus: FocusViewModel = viewModel(factory = factory)
                    val state by focus.uiState.collectAsStateWithLifecycle()
                    LifecycleResumeEffect(focus) {
                        focus.refresh()
                        onPauseOrDispose { }
                    }
                    FocusScreen(
                        state,
                        focus::start,
                        focus::stop,
                        focus::setKeepScreenOn,
                        focus::dismissResult,
                        focus::refresh
                    )
                }
                val limitsContent: @androidx.compose.runtime.Composable (() -> Unit) -> Unit = { back ->
                    val factory = remember(container) {
                        viewModelFactory { initializer { container.appLimitsViewModel() } }
                    }
                    val limits: AppLimitsViewModel = viewModel(factory = factory)
                    val state by limits.uiState.collectAsStateWithLifecycle()
                    LifecycleResumeEffect(limits) {
                        limits.refresh()
                        onPauseOrDispose { }
                    }
                    AppLimitsScreen(state, limits::save, back, limits::refresh)
                }
                when {
                    // A debug build launched with a screen extra wins, so the diagnostic
                    // screens stay reachable even before onboarding was completed.
                    debugDestination != null -> ScreenGardenApp(
                        debugDestination = debugDestination,
                        gardenContent = gardenContent,
                        focusContent = focusContent,
                        limitsContent = limitsContent
                    )
                    onboardingDone == null -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                    onboardingDone == false -> {
                        // The flow starts from what is already stored, so the sliders
                        // open on the user's real values rather than on hardcoded ones.
                        val dailyGoalMs by settingsRepository.dailyGoalMs
                            .collectAsStateWithLifecycle(initialValue = SettingsDefaults.DAILY_GOAL_MS)
                        val pickupGoal by settingsRepository.pickupGoal
                            .collectAsStateWithLifecycle(initialValue = SettingsDefaults.PICKUP_GOAL)
                        val bedtimeStart by settingsRepository.bedtimeStart
                            .collectAsStateWithLifecycle(initialValue = SettingsDefaults.BEDTIME_START)
                        val bedtimeEnd by settingsRepository.bedtimeEnd
                            .collectAsStateWithLifecycle(initialValue = SettingsDefaults.BEDTIME_END)
                        OnboardingScreen(
                            initialChoices = OnboardingChoices.fromStored(
                                dailyGoalMs = dailyGoalMs,
                                pickupGoal = pickupGoal,
                                bedtimeStart = bedtimeStart,
                                bedtimeEnd = bedtimeEnd
                            ),
                            isUsageAccessGranted = { UsageAccess.isGranted(appContext) },
                            onOpenUsageAccessSettings = { startActivity(UsageAccess.settingsIntent()) },
                            onNotificationsChosen = { allowed ->
                                lifecycleScope.launch { onboardingCompletion.rememberNotifications(allowed) }
                            },
                            onComplete = { choices ->
                                lifecycleScope.launch { onboardingCompletion.complete(choices) }
                            }
                        )
                    }
                    else -> ScreenGardenApp(
                        gardenContent = gardenContent,
                        focusContent = focusContent,
                        limitsContent = limitsContent
                    )
                }
            }
        }
    }

    /** The debug screen requested by an intent extra, or null outside debug builds. */
    private fun requestedDebugScreen(): String? =
        if (isDebugExtraAllowed()) intent.getStringExtra(EXTRA_DEBUG_SCREEN) else null

    /**
     * True only in a debuggable build. Every debug hook is gated on this, so
     * shipped behaviour depends solely on stored settings and normal navigation.
     */
    private fun isDebugExtraAllowed(): Boolean = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private companion object {
        /** Launch extra: `adb shell am start ... --ez amoled true`. Debug only. */
        const val EXTRA_AMOLED = "amoled"

        /** Launch extra: `adb shell am start ... --es screen usage_self_check`. */
        const val EXTRA_DEBUG_SCREEN = "screen"
    }
}
