// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settingsRepository = (application as ScreenGardenApplication).container.settingsRepository
        val amoledPreview = isAmoledPreviewRequested()
        setContent {
            // SYSTEM is only the initial value: it covers the few milliseconds
            // before DataStore emits the stored choice (K31).
            val themePreference by settingsRepository.theme
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
            ScreenGardenTheme(
                themePreference = themePreference,
                amoled = amoledPreview
            ) {
                ScreenGardenApp()
            }
        }
    }

    /**
     * True only in a debuggable build launched with `--ez amoled true`.
     *
     * The AMOLED scheme is now a persisted setting, but its picker arrives in P22
     * and screenshot tooling needs the variant before that, so the debug override
     * stays until then (D35, K28). A release build always returns false, which
     * means shipped theme behaviour depends only on the stored preference.
     */
    private fun isAmoledPreviewRequested(): Boolean {
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return debuggable && intent.getBooleanExtra(EXTRA_AMOLED, false)
    }

    private companion object {
        /** Launch extra: `adb shell am start ... --ez amoled true`. Debug only. */
        const val EXTRA_AMOLED = "amoled"
    }
}
