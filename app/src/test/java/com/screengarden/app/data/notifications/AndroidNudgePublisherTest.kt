// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.notifications

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.domain.notifications.Nudge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AndroidNudgePublisherTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val publisher = AndroidNudgePublisher(context) { "Reading" }

    @Before fun grantFakePermission() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test fun dailyAndAppMessagesUseSeparateChannelsAndIncludeTheMilestone() {
        publisher.post(Nudge(50))
        publisher.post(Nudge(80, "fake.reader"))
        val notifications = manager.activeNotifications.map { it.notification }
        assertEquals(2, notifications.size)
        assertEquals(2, notifications.map { it.channelId }.distinct().size)
        val texts = notifications.map { it.extras.getCharSequence(Notification.EXTRA_TEXT).toString() }
        assertTrue(texts.any { "50%" in it })
        assertTrue(texts.any { "80%" in it && "Reading" in it })
        assertTrue(notifications.all { it.actions.single().actionIntent != null })
    }

    @Test fun deniedPermissionPreventsPublishing() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(publisher.canPost())
        publisher.post(Nudge(100))
        assertTrue(manager.activeNotifications.isEmpty())
    }

    @Test fun creatingChannelsPreservesAUsersMutedChannel() {
        manager.createNotificationChannel(
            NotificationChannel(
                AndroidNudgePublisher.DAILY_CHANNEL,
                "Muted fake channel",
                NotificationManager.IMPORTANCE_NONE
            )
        )
        publisher.createChannels()
        assertFalse(publisher.canPost(Nudge(50)))
        assertTrue(publisher.canPost(Nudge(50, "fake.reader")))
        assertEquals(
            NotificationManager.IMPORTANCE_LOW,
            manager.getNotificationChannel(AndroidNudgePublisher.APP_CHANNEL).importance
        )
    }
}
