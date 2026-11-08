// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.screengarden.app.MainActivity
import com.screengarden.app.R
import com.screengarden.app.domain.notifications.Nudge

/** Optional low-importance notifications with separately controllable daily and app channels. */
class AndroidNudgePublisher(context: Context, private val appLabel: (String) -> String) : NudgePublisher {
    private val context = context.applicationContext
    private val manager = this.context.getSystemService(NotificationManager::class.java)

    fun createChannels() {
        manager.createNotificationChannels(
            listOf(
                channel(DAILY_CHANNEL, R.string.nudge_daily_channel, R.string.nudge_daily_channel_description),
                channel(APP_CHANNEL, R.string.nudge_app_channel, R.string.nudge_app_channel_description)
            )
        )
    }

    override fun canPost(nudge: Nudge?): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (nudge == null) return true
        createChannels()
        return manager.getNotificationChannel(channelId(nudge)).importance != NotificationManager.IMPORTANCE_NONE
    }

    override fun post(nudge: Nudge): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (!canPost(nudge)) return false
        val label = nudge.packageName?.let(appLabel)
        val message = if (label == null) {
            context.getString(
                if (nudge.threshold == 100) R.string.nudge_daily_reached else R.string.nudge_daily_progress,
                nudge.threshold
            )
        } else {
            context.getString(
                if (nudge.threshold == 100) R.string.nudge_app_reached else R.string.nudge_app_progress,
                label,
                nudge.threshold
            )
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val controls = PendingIntent.getActivity(
            context,
            channelId(nudge).hashCode(),
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId(nudge))
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId(nudge))
            .setSmallIcon(R.drawable.ic_focus_notification)
            .setContentTitle(context.getString(R.string.nudge_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, context.getString(R.string.nudge_manage), controls)
            .build()
        return try {
            // One visible notification per scope; later milestones replace earlier ones.
            manager.notify("nudge:${nudge.packageName ?: "daily"}", 202, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun channel(id: String, name: Int, description: Int): NotificationChannel = NotificationChannel(
        id,
        context.getString(name),
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        this.description = context.getString(description)
        setShowBadge(false)
    }

    private fun channelId(nudge: Nudge): String = if (nudge.packageName == null) DAILY_CHANNEL else APP_CHANNEL

    companion object {
        const val DAILY_CHANNEL = "daily_goal_nudges"
        const val APP_CHANNEL = "app_limit_nudges"
    }
}
