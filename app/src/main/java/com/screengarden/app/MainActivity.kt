// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.screengarden.app.debug.DebugCatalog
import com.screengarden.app.domain.model.ThemePreference
import com.screengarden.app.ui.navigation.ScreenGardenApp
import com.screengarden.app.ui.theme.ScreenGardenTheme

/**
 * The app's only Activity. All UI is Compose; navigation lives in
 * [com.screengarden.app.ui.navigation].
 *
 * The Activity is also the composition root for the theme: the persisted
 * preference is read here and handed to [ScreenGardenTheme], because the themed
 * shell cannot be drawn before the setting is known. Screens themselves still
 * never touch the data layer (SECTION 4 layer rules).
 *
 * Debug-only extras are read here too, but only in a debuggable build, so a release
 * APK cannot be steered into a diagnostic screen (D49).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as ScreenGardenApplication).container
        val settingsRepository = container.settingsRepository
        val amoledPreview = isDebugExtraAllowed() && intent.getBooleanExtra(EXTRA_AMOLED, false)
        // DebugCatalog.destination returns null in release builds (the release
        // source set has a stub), so this is a no-op there.
        val debugDestination = requestedDebugScreen()?.let { DebugCatalog.destination(it, this) }
        setContent {
            // SYSTEM is only the initial value: it covers the few milliseconds
            // before DataStore emits the stored choice (K31).
            val themePreference by settingsRepository.theme
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
            ScreenGardenTheme(
                themePreference = themePreference,
                amoled = amoledPreview
            ) {
                ScreenGardenApp(debugDestination = debugDestination)
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
