// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.focus

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.screengarden.app.MainActivity
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.data.usage.UsageAccess
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
    fun completedTimerPlantsAndRewardsOnlyOnce() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = (context.applicationContext as ScreenGardenApplication).container
        container.database.focusSessionDao().deleteAll()
        container.database.dailyStatDao().deleteAll()
        container.database.plantDao().getAll().forEach { container.database.plantDao().deleteById(it.plantId) }
        // Fake presentation prevents the real Garden screen from backfilling old-day plant rewards.
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).putExtra("screen", "focus_demo")
        ).use { activity ->
            activity.onActivity { container.focusController.start(1, FocusTag.READING) }
            awaitCondition { container.focusSessionRepository.activeSession() != null }
            val id = requireNotNull(container.focusSessionRepository.activeSession()).id
            // Remain in our own UI; screen-on alone is not a distraction.
            delay(62_000)
            awaitCondition { container.focusSessionRepository.activeSession() == null }
            val completed = requireNotNull(container.focusSessionRepository.getSession(id))
            assertEquals(FocusStatus.COMPLETED, completed.status)
            assertEquals(60_000L, completed.actualDurationMs)
            assertEquals(2, container.database.plantDao().getAll().single().growthPoints)
            assertTrue(!container.focusSessionRepository.checkpoint(completed))
            assertEquals(2, container.database.plantDao().getAll().single().growthPoints)
        }
    }

    @Test
    fun otherAppUsageAfterGraceEndsSoftlyWithoutLosingThePlant() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = (context.applicationContext as ScreenGardenApplication).container
        assertTrue("Grant usage access on the dedicated emulator before this test", UsageAccess.isGranted(context))
        container.database.focusSessionDao().deleteAll()
        container.database.dailyStatDao().deleteAll()
        container.database.plantDao().getAll().forEach { container.database.plantDao().deleteById(it.plantId) }
        container.gardenRepository.savePlant(
            com.screengarden.app.domain.garden.Plant(
                "fake",
                "fern",
                12,
                60,
                com.screengarden.app.domain.garden.PlantCondition.GROWING,
                System.currentTimeMillis()
            )
        )
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).putExtra("screen", "focus_demo")
        ).use { activity ->
            activity.onActivity { container.focusController.start(1, FocusTag.STUDY) }
            awaitCondition { container.focusSessionRepository.activeSession() != null }
            val id = requireNotNull(container.focusSessionRepository.activeSession()).id
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            delay(20_000)
            assertEquals(FocusStatus.RUNNING, container.focusSessionRepository.getSession(id)?.status)
            delay(25_000)
            awaitCondition { container.focusSessionRepository.activeSession() == null }
            assertEquals(FocusStatus.FAILED_SOFT, container.focusSessionRepository.getSession(id)?.status)
            val plant = container.database.plantDao().getAll().single()
            assertEquals("fake", plant.plantId)
            assertEquals(11, plant.growthPoints)
            assertEquals(60, plant.health)
        }
    }

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
