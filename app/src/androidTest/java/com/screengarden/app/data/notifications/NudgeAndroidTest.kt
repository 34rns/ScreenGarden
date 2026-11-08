// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.core.time.BedtimeWindow
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.data.usage.AppUsage
import com.screengarden.app.data.usage.DayUsage
import com.screengarden.app.data.usage.DayUsageResult
import com.screengarden.app.data.usage.UnlockCount
import com.screengarden.app.data.work.NudgeScheduler
import com.screengarden.app.domain.limits.AppLimitRule
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Only fake usage and app names; run on the dedicated emulator. */
@RunWith(AndroidJUnit4::class)
class NudgeAndroidTest {
    @Test fun nativeNotificationsUseTheLedgerAndOpenChannelControls() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()
        val fakeNow = Instant.parse("2000-01-01T12:00:00Z").toEpochMilli()
        val day = LocalDay.startOfDay(fakeNow, ZoneOffset.UTC)
        context.getSharedPreferences(
            "nudge_delivery_flags",
            Context.MODE_PRIVATE
        ).edit().remove(day.toString()).commit()
        val publisher = AndroidNudgePublisher(context) { "Reading" }
        fun recreatedCheck(): GentleNudgeCheck = GentleNudgeCheck(
            preferences = { NudgePreferences(true, 1000, BedtimeWindow(LocalTime.NOON, LocalTime.NOON)) },
            readUsage = { key ->
                DayUsageResult.Available(
                    DayUsage(
                        key,
                        500,
                        0,
                        UnlockCount.Unsupported,
                        listOf(AppUsage("fake.reader", 30_000, 1, fakeNow)),
                        true
                    )
                )
            },
            readLimits = { listOf(AppLimitRule("fake.reader", 1)) },
            ledger = PreferencesNudgeLedger(context),
            publisher = publisher,
            nowMillis = { fakeNow },
            zoneId = { ZoneOffset.UTC }
        )
        assertEquals(2, recreatedCheck().run())
        assertEquals(0, recreatedCheck().run())
        delay(500)
        val active = manager.activeNotifications.filter { it.tag.startsWith("nudge:") }
        assertEquals(2, active.size)
        assertTrue(active.all { "50%" in it.notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString() })
        val daily = active.single { it.notification.channelId == AndroidNudgePublisher.DAILY_CHANNEL }
        daily.notification.actions.single().actionIntent.send()
        var channelControlsVisible = false
        repeat(12) {
            delay(500)
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (root?.findAccessibilityNodeInfosByText(
                    context.getString(com.screengarden.app.R.string.nudge_daily_channel)
                )
                    ?.isNotEmpty() == true
            ) {
                channelControlsVisible = true
            }
        }
        assertTrue("Channel settings should open from Manage reminders", channelControlsVisible)
        manager.cancelAll()
    }

    @Test fun periodicSchedulingIsIdempotentOnAndroid() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = (context.applicationContext as ScreenGardenApplication).container
        container.nudgeScheduler.schedule()
        container.nudgeScheduler.schedule()
        val jobs = WorkManager.getInstance(context).getWorkInfosForUniqueWork(NudgeScheduler.PERIODIC_NAME)
            .get(15, TimeUnit.SECONDS)
        assertEquals(1, jobs.size)
        assertTrue(!jobs.single().state.isFinished)
    }
}
