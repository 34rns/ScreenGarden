// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.focus

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.screengarden.app.MainActivity
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.domain.focus.FocusStatus
import com.screengarden.app.domain.focus.FocusTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on the dedicated emulator; all session inputs are fake. */
@RunWith(AndroidJUnit4::class)
class FocusTimerServiceTest {
    @Test
    fun timerSurvivesLeavingActivityAndNotificationStopPersistsCancellation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = (context.applicationContext as ScreenGardenApplication).container
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                context.packageName,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }
        container.database.focusSessionDao().deleteAll()
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            activity.onActivity { container.focusController.start(1, FocusTag.STUDY) }
            awaitCondition { container.focusSessionRepository.activeSession() != null }
            // A second start must reuse the timer and still allow it to stop cleanly.
            activity.onActivity { container.focusController.start(1, FocusTag.WORK) }
            val id = requireNotNull(container.focusSessionRepository.activeSession()).id
            context.startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            delay(2500)
            assertTrue(requireNotNull(container.focusSessionRepository.getSession(id)).actualDurationMs > 0)
            val notifications = context.getSystemService(NotificationManager::class.java)
            val timer = notifications.activeNotifications.first { it.notification.actions?.isNotEmpty() == true }
            timer.notification.actions.first().actionIntent.send()
            awaitCondition { container.focusSessionRepository.activeSession() == null }
            assertEquals(FocusStatus.CANCELLED, container.focusSessionRepository.getSession(id)?.status)
            awaitCondition { notifications.activeNotifications.isEmpty() }
        }
    }

    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        repeat(100) {
            if (condition()) return
            delay(100)
        }
        error("Emulator condition did not become true")
    }
}
