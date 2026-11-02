// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.screengarden.app.ui.navigation.ScreenGardenApp
import com.screengarden.app.ui.theme.ScreenGardenTheme

/**
 * The app's only Activity. All UI is Compose; navigation lives in
 * [com.screengarden.app.ui.navigation].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ScreenGardenTheme(amoled = isAmoledPreviewRequested()) {
                ScreenGardenApp()
            }
        }
    }

    /**
     * True only in a debuggable build launched with `--ez amoled true`.
     *
     * The AMOLED scheme has no Settings toggle yet (K1), and screenshot tooling
     * cannot otherwise reach it. This hook exists so the true-black variant can be
     * captured before P22 ships the preference; a release build always returns
     * false, so shipped theme behaviour is unchanged (D35).
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
