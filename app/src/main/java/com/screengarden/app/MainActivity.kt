// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.screengarden.app.data.backup.BackupDocuments
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
import com.screengarden.app.ui.screens.garden.LocalPlantRenderer
import com.screengarden.app.ui.screens.garden.PackPlantRenderer
import com.screengarden.app.ui.screens.garden.PlantRenderer
import com.screengarden.app.ui.screens.garden.VectorPlantRenderer
import com.screengarden.app.ui.screens.habits.HabitsScreen
import com.screengarden.app.ui.screens.habits.HabitsViewModel
import com.screengarden.app.ui.screens.insights.GardenHistoryScreen
import com.screengarden.app.ui.screens.insights.GardenHistoryViewModel
import com.screengarden.app.ui.screens.insights.InsightsScreen
import com.screengarden.app.ui.screens.insights.InsightsViewModel
import com.screengarden.app.ui.screens.limits.AppLimitsScreen
import com.screengarden.app.ui.screens.limits.AppLimitsViewModel
import com.screengarden.app.ui.screens.settings.BackupControls
import com.screengarden.app.ui.screens.settings.BackupViewModel
import com.screengarden.app.ui.screens.settings.SettingsActions
import com.screengarden.app.ui.screens.settings.SettingsScreen
import com.screengarden.app.ui.screens.settings.SettingsViewModel
import com.screengarden.app.ui.theme.ScreenGardenTheme
import kotlinx.coroutines.CancellationException
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
        // DebugCatalog.destination returns null in release builds (the release
        // source set has a stub), so this is a no-op there.
        val debugDestination = requestedDebugScreen()?.let { DebugCatalog.destination(it, this) }
        // Fire-and-forget: the worker reports through logcat, and the UI shows stored
        // data, so a slow sync must not delay the first frame.
        container.usageSyncScheduler.syncNow()
        container.nudgeScheduler.checkNow()
        setContent {
            var recoveryDone by remember { mutableStateOf(debugDestination != null) }
            var recoveryError by remember { mutableStateOf(false) }
            var recoveryAttempt by remember { mutableStateOf(0) }
            LaunchedEffect(container, recoveryAttempt) {
                if (!recoveryDone) {
                    recoveryError = false
                    try {
                        container.backupRepository.recoverPendingSettings()
                        recoveryDone = true
                    } catch (failure: Exception) {
                        if (failure is CancellationException) throw failure
                        recoveryError = true
                    }
                }
            }
            val saveSnapshot = rememberSnapshotSaveAction()
            val plantRenderer by produceState<PlantRenderer>(VectorPlantRenderer, container) {
                value = PackPlantRenderer(container.plantPacks().packs.associate { it.metadata.id to it.images })
            }
            // SYSTEM is only the initial value: it covers the few milliseconds
            // before DataStore emits the stored choice (K31).
            val themePreference by settingsRepository.theme
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
            val accentColor by settingsRepository.accentColor.collectAsStateWithLifecycle(SettingsDefaults.ACCENT_COLOR)
            val displayedTheme = debugDestination?.themeOverride ?: themePreference
            val darkBars = displayedTheme.prefersDark(isSystemInDarkTheme())
            SideEffect {
                // App-selected dark themes can differ from the device's system theme.
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkBars
                    isAppearanceLightNavigationBars = !darkBars
                }
            }
            ScreenGardenTheme(
                themePreference = displayedTheme,
                accentColor = accentColor
            ) {
                if (!recoveryDone) {
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp)) {
                        if (recoveryError) {
                            Text(stringResource(R.string.backup_recovery_error))
                            Button({ recoveryAttempt++ }) { Text(stringResource(R.string.backup_retry)) }
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                    return@ScreenGardenTheme
                }
                CompositionLocalProvider(LocalPlantRenderer provides plantRenderer) {
                    // null means DataStore has not answered yet. Drawing nothing for that
                    // single frame is deliberate: starting at `false` would flash the
                    // first-run flow at every returning user (D53).
                    // The map is hoisted into remember: a Flow operator inside composition
                    // builds a new Flow on every recomposition and restarts the collector.
                    val onboardingDoneFlow = remember(settingsRepository) {
                        settingsRepository.onboardingDone.map<Boolean, Boolean?> { done -> done }
                    }
                    val onboardingDone by onboardingDoneFlow.collectAsStateWithLifecycle(initialValue = null)
                    val settingsContent: @androidx.compose.runtime.Composable (
                        () -> Unit,
                        () -> Unit
                    ) -> Unit = { limits, habits ->
                        val factory =
                            remember(container) { viewModelFactory { initializer { container.settingsViewModel() } } }
                        val settings: SettingsViewModel = viewModel(factory = factory)
                        val state by settings.uiState.collectAsStateWithLifecycle()
                        val backupFactory = remember(container) {
                            viewModelFactory { initializer { container.backupViewModel() } }
                        }
                        val backup: BackupViewModel = viewModel(factory = backupFactory)
                        val backupState by backup.uiState.collectAsStateWithLifecycle()
                        val documents = remember { BackupDocuments(contentResolver) }
                        val saveBackup = rememberLauncherForActivityResult(
                            ActivityResultContracts.CreateDocument("application/json")
                        ) { uri ->
                            if (uri == null) backup.cancelExport() else backup.save { documents.write(uri, it) }
                        }
                        val loadBackup =
                            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                                if (uri != null) backup.load { documents.read(uri) }
                            }
                        var allowed by remember {
                            mutableStateOf(NotificationManagerCompat.from(appContext).areNotificationsEnabled())
                        }
                        val permission =
                            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                                allowed =
                                    granted &&
                                    NotificationManagerCompat.from(appContext).areNotificationsEnabled()
                                settings.setNotifications(granted)
                            }
                        LifecycleResumeEffect(settings) {
                            allowed = NotificationManagerCompat.from(appContext).areNotificationsEnabled()
                            onPauseOrDispose { }
                        }
                        SettingsScreen(
                            limits,
                            onMilestones = habits,
                            state = state,
                            actions = SettingsActions(
                                saveGoals = settings::saveGoals,
                                setTheme = settings::setTheme,
                                setAccent = settings::setAccent,
                                setNotifications = { enabled ->
                                    if (enabled &&
                                        Build.VERSION.SDK_INT >= 33 &&
                                        ContextCompat.checkSelfPermission(
                                            appContext,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) !=
                                        PackageManager.PERMISSION_GRANTED
                                    ) {
                                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        settings.setNotifications(enabled)
                                    }
                                },
                                setKeepScreenOn = settings::setKeepScreenOn,
                                setHemisphere = settings::setHemisphere,
                                setLowPower = settings::setLowPower,
                                showIntroduction = settings::showIntroduction
                            ),
                            notificationsAllowed = allowed,
                            onNotificationSettings = {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                    ).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                )
                            },
                            versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty(),
                            onAppSettings = {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        "package:$packageName".toUri()
                                    )
                                )
                            },
                            backupContent = {
                                BackupControls(
                                    backupState,
                                    { backup.prepareExport { saveBackup.launch("ScreenGarden-backup.json") } },
                                    {
                                        loadBackup.launch(
                                            arrayOf("application/json", "text/plain", "application/octet-stream")
                                        )
                                    },
                                    backup::restore,
                                    backup::cancelPreview
                                )
                            }
                        )
                    }
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
                            onRetry = garden::refresh,
                            onSaveSnapshot = saveSnapshot
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
                    val habitsContent: @androidx.compose.runtime.Composable (() -> Unit) -> Unit = { back ->
                        val factory =
                            remember(container) { viewModelFactory { initializer { container.habitsViewModel() } } }
                        val habits: HabitsViewModel = viewModel(factory = factory)
                        val state by habits.uiState.collectAsStateWithLifecycle()
                        HabitsScreen(
                            state,
                            habits::setCompassionDay,
                            habits::setThreshold,
                            habits::setMonthlyLimit,
                            back
                        )
                    }
                    val historyContent: @androidx.compose.runtime.Composable (() -> Unit) -> Unit = { back ->
                        val factory = remember(container) {
                            viewModelFactory { initializer { container.gardenHistoryViewModel() } }
                        }
                        val history: GardenHistoryViewModel = viewModel(factory = factory)
                        val state by history.uiState.collectAsStateWithLifecycle()
                        GardenHistoryScreen(state, history::setPeriod, history::refresh, back)
                    }
                    val insightsContent: @androidx.compose.runtime.Composable (() -> Unit) -> Unit = { history ->
                        val factory = remember(container) {
                            viewModelFactory { initializer { container.insightsViewModel() } }
                        }
                        val insights: InsightsViewModel = viewModel(factory = factory)
                        val state by insights.uiState.collectAsStateWithLifecycle()
                        LifecycleResumeEffect(insights) {
                            insights.refresh()
                            onPauseOrDispose { }
                        }
                        InsightsScreen(
                            state,
                            insights::setPeriod,
                            insights::refresh,
                            { startActivity(UsageAccess.settingsIntent()) },
                            onHistory = history
                        )
                    }
                    when {
                        // A debug build launched with a screen extra wins, so the diagnostic
                        // screens stay reachable even before onboarding was completed.
                        debugDestination != null -> ScreenGardenApp(
                            debugDestination = debugDestination,
                            gardenContent = gardenContent,
                            focusContent = focusContent,
                            limitsContent = limitsContent,
                            habitsContent = habitsContent,
                            insightsContent = insightsContent,
                            historyContent = historyContent,
                            settingsContent = settingsContent
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
                            limitsContent = limitsContent,
                            habitsContent = habitsContent,
                            insightsContent = insightsContent,
                            historyContent = historyContent,
                            settingsContent = settingsContent
                        )
                    }
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
        /** Launch extra: `adb shell am start ... --es screen usage_self_check`. */
        const val EXTRA_DEBUG_SCREEN = "screen"
    }
}
