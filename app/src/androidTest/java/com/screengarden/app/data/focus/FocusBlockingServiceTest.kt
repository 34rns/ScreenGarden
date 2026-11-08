// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.focus

import android.app.UiAutomation
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.screengarden.app.MainActivity
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.domain.focus.FocusBlockPolicy
import com.screengarden.app.domain.focus.FocusTag
import java.io.FileInputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Only the dedicated emulator and artificial timer/list are used; no screenshots or event logs. */
@RunWith(AndroidJUnit4::class)
class FocusBlockingServiceTest {
    @Test
    fun optInRedirectsAndAllowDisableAndEndKeepAppsAvailable() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val context = instrumentation.targetContext
        val container = (context.applicationContext as ScreenGardenApplication).container
        fun shell(command: String): String = automation.executeShellCommand(command).use {
            FileInputStream(it.fileDescriptor).bufferedReader().readText().trim()
        }
        val previous = shell("settings get secure enabled_accessibility_services")
        val previousEnabled = shell("settings get secure accessibility_enabled")
        val oldPolicy = container.focusBlockStore.observePolicy().first()
        val app = "com.google.android.calendar"
        val launch = requireNotNull(context.packageManager.getLaunchIntentForPackage(app))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        suspend fun openAndCheck(blocked: Boolean) {
            shell("input keyevent KEYCODE_HOME")
            delay(300)
            context.startActivity(launch)
            delay(2200)
            val foreground = shell("dumpsys activity activities")
                .lineSequence().filter { it.contains("topResumedActivity") || it.contains("mResumedActivity") }
                .joinToString()
            assertTrue("Blocked=$blocked must match the current foreground", foreground.contains(app) != blocked)
        }
        try {
            container.database.focusSessionDao().deleteAll()
            container.focusBlockStore.save(FocusBlockPolicy())
            val component = "${context.packageName}/${FocusBlockingService::class.java.name}"
            shell("settings put secure enabled_accessibility_services $component")
            shell("settings put secure accessibility_enabled 1")
            repeat(30) {
                if (!isFocusBlockingGranted(context)) delay(100)
            }
            assertTrue(isFocusBlockingGranted(context))
            delay(1500)
            ActivityScenario.launch<MainActivity>(
                Intent(context, MainActivity::class.java).putExtra("screen", "focus_demo")
            ).use { activity ->
                activity.onActivity { container.focusController.start(5, FocusTag.STUDY) }
                delay(2500)
                openAndCheck(false)
                container.focusBlockStore.save(FocusBlockPolicy(true, setOf(app)))
                delay(500)
                openAndCheck(true)
                container.focusBlockStore.save(FocusBlockPolicy(true, setOf(app), setOf(app)))
                delay(500)
                openAndCheck(false)
                container.focusBlockStore.save(FocusBlockPolicy(false, setOf(app)))
                delay(500)
                openAndCheck(false)
                container.focusBlockStore.save(FocusBlockPolicy(true, setOf(app)))
                container.focusSessionRepository.activeSession()?.let { container.focusController.stop(it.id) }
                delay(1500)
                openAndCheck(false)
            }
        } finally {
            container.focusSessionRepository.activeSession()?.let { container.focusController.stop(it.id) }
            container.focusBlockStore.save(oldPolicy)
            if (previous == "null") {
                shell("settings delete secure enabled_accessibility_services")
            } else {
                shell("settings put secure enabled_accessibility_services $previous")
            }
            if (previousEnabled == "null") {
                shell("settings delete secure accessibility_enabled")
            } else {
                shell("settings put secure accessibility_enabled $previousEnabled")
            }
        }
    }
}
