// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.focus

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.data.usage.LaunchableAppCatalog
import com.screengarden.app.domain.focus.FocusBlockPolicy
import com.screengarden.app.domain.focus.FocusSessionState
import com.screengarden.app.domain.focus.hasFreshFocus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Opt-in package-only redirect. Never read or retain event text, window content,
 * accessibility nodes or observed package names; never log an accessibility event.
 */
class FocusBlockingService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var policy = FocusBlockPolicy()
    private var sessions: List<FocusSessionState> = emptyList()
    private var protected: Set<String> = emptySet()
    private var launchable: Set<String> = emptySet()
    private var bootCount = -1

    override fun onServiceConnected() {
        bootCount = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)
        val container = (application as ScreenGardenApplication).container
        scope.launch {
            try {
                val inventory = withContext(Dispatchers.IO) {
                    protectedFocusApps(this@FocusBlockingService) to
                        LaunchableAppCatalog(this@FocusBlockingService).installedApps().map { it.packageName }.toSet()
                }
                protected = inventory.first
                launchable = inventory.second
                container.backupRepository.recoverPendingSettings()
                combine(container.focusBlockStore.observePolicy(), container.focusSessionRepository.observeRunning()) {
                        settings,
                        running
                    ->
                    settings to running
                }.collect { (settings, running) ->
                    policy = settings
                    sessions = running
                }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                policy = FocusBlockPolicy()
                sessions = emptyList()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() !in launchable) return
        // packageName is an ephemeral comparison only; canRetrieveWindowContent is false in XML.
        if (policy.shouldRedirect(
                event.packageName?.toString(),
                hasFreshFocus(sessions, System.currentTimeMillis(), bootCount),
                protected
            )
        ) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    override fun onInterrupt() {
        policy = FocusBlockPolicy()
        sessions = emptyList()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

/** Permission status is read at resume; enabling Android's service alone never enables blocking. */
fun isFocusBlockingGranted(context: Context): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
    val component = ComponentName(context, FocusBlockingService::class.java)
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
        ComponentName.unflattenFromString(it.id) == component
    }
}

/** Keep escape routes and calling apps available even if a backup supplies their package names. */
fun protectedFocusApps(context: Context): Set<String> = buildSet {
    addAll(
        listOf(
            context.packageName,
            "com.screengarden.app",
            "com.screengarden.app.debug",
            "android",
            "com.android.settings",
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller"
        )
    )
    for (intent in listOf(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
        Intent(Intent.ACTION_DIAL),
        Intent(Settings.ACTION_SETTINGS)
    )) {
        context.packageManager.queryIntentActivities(intent, 0).forEach { add(it.activityInfo.packageName) }
    }
}
