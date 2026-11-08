// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui

import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.screengarden.app.MainActivity
import com.screengarden.app.R
import com.screengarden.app.ScreenGardenApplication
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Real route reacts to local preferences; public screenshots use separate fake-only destinations. */
@RunWith(AndroidJUnit4::class)
class BedtimeAndroidTest {
    @Test fun openingDuringTheWindowShowsSleepGardenAndSettingsChangesRestoreDaytime() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val settings = (context.applicationContext as ScreenGardenApplication).container.settingsRepository
        val savedStart = settings.bedtimeStart.first()
        val savedEnd = settings.bedtimeEnd.first()
        val savedIntro = settings.onboardingDone.first()
        fun containsTitle(node: AccessibilityNodeInfo?, title: String): Boolean = node != null &&
            (
                node.text?.toString() == title ||
                    (0 until node.childCount).any {
                        containsTitle(node.getChild(it), title)
                    }
                )
        suspend fun waitForTitle(id: Int): Boolean {
            repeat(20) {
                delay(500)
                val root = instrumentation.uiAutomation.rootInActiveWindow
                // Compose virtual text nodes are exposed through traversal, not the framework text-search hook.
                if (containsTitle(root, context.getString(id))) return true
            }
            return false
        }
        try {
            val clock = LocalTime.now()
            val format = DateTimeFormatter.ofPattern("HH:mm")
            settings.setBedtimeStart(clock.minusHours(1).format(format))
            settings.setBedtimeEnd(clock.plusHours(1).format(format))
            settings.setOnboardingDone(true)
            context.startActivity(
                Intent(context, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            )
            assertTrue("Normal Garden route should open in sleep mode", waitForTitle(R.string.bedtime_title))
            settings.setBedtimeStart("00:00")
            settings.setBedtimeEnd("00:00")
            assertTrue("Disabling the window should restore Garden", waitForTitle(R.string.garden_title))
        } finally {
            settings.setBedtimeStart(savedStart)
            settings.setBedtimeEnd(savedEnd)
            settings.setOnboardingDone(savedIntro)
        }
    }
}
