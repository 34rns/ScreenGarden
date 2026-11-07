// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.screengarden.app.MainActivity
import com.screengarden.app.R
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.focus.FocusSessionMachine
import com.screengarden.app.domain.focus.FocusSessionState
import com.screengarden.app.domain.focus.FocusStatus
import com.screengarden.app.domain.focus.FocusTag
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Local foreground timer. No wake lock, exact alarm, network or automatic reboot start. */
class FocusTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commands = Mutex()
    private var timer: Job? = null
    private var runtimeSession: FocusSessionState? = null
    private var runtimeStartElapsedMs = 0L
    private var runtimeActualMs = 0L
    private val repository get() = (application as ScreenGardenApplication).container.focusSessionRepository
    private val manager get() = getSystemService(NotificationManager::class.java)

    override fun onCreate() {
        super.onCreate()
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                getString(R.string.focus_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.focus_channel_description) }
        )
        // Start before database I/O; Android requires this promptly after a user launch.
        ServiceCompat.startForeground(
            this,
            NOTIFICATION,
            notification(null),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            commands.withLock {
                try {
                    if (intent?.action == ACTION_STOP) {
                        stopRequested(intent.getStringExtra(EXTRA_SESSION))
                    } else {
                        startOrRecover(intent)
                    }
                } catch (failure: Exception) {
                    if (failure is CancellationException) throw failure
                    // Failed storage must not leave an apparently running, unrecorded timer.
                    timer?.cancel()
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private suspend fun startOrRecover(intent: Intent?) {
        val now = System.currentTimeMillis()
        val boot = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, 0)
        var session = repository.activeSession()
        if (session != null && runtimeSession?.id != session.id) {
            session = FocusSessionMachine.recover(session, now, boot)
            repository.checkpoint(session)
            if (session.status != FocusStatus.RUNNING) session = null
        }
        if (session == null && intent?.action == ACTION_START) {
            val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
            if (minutes !in 1..240) {
                stopSelf()
                return
            }
            val tag = FocusTag.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_TAG) } ?: FocusTag.OTHER
            session = repository.start(
                FocusSessionState(
                    id = UUID.randomUUID().toString(),
                    dayStartUtcMillis = LocalDay.startOfDay(now),
                    startedAtUtcMillis = now,
                    plannedMinutes = minutes,
                    tag = tag,
                    bootCount = boot,
                    plantId = intent.getStringExtra(EXTRA_PLANT)
                )
            )
        }
        if (session == null) {
            stopSelf()
            return
        }
        if (runtimeSession?.id == session.id && timer?.isActive == true) return
        timer?.cancel()
        runtimeSession = session
        runtimeStartElapsedMs = SystemClock.elapsedRealtime()
        runtimeActualMs = session.actualDurationMs
        manager.notify(NOTIFICATION, notification(session))
        val running = session
        timer = scope.launch {
            var lastShownMinute = -1L
            try {
                while (true) {
                    delay(1000)
                    val keepRunning = commands.withLock {
                        val progressed = FocusSessionMachine.tick(running, actualNow(), System.currentTimeMillis())
                        if (!repository.checkpoint(progressed)) {
                            stopSelf()
                            return@withLock false
                        }
                        runtimeSession = progressed
                        if (progressed.status != FocusStatus.RUNNING) {
                            stopSelf()
                            return@withLock false
                        }
                        // Countdown updates once per minute; persistence stays local and small.
                        val minute = (progressed.remainingDurationMs + 59_999) / 60_000
                        if (minute != lastShownMinute) {
                            manager.notify(NOTIFICATION, notification(progressed))
                            lastShownMinute = minute
                        }
                        true
                    }
                    if (!keepRunning) break
                }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                stopSelf()
            }
        }
    }

    private fun actualNow(): Long =
        runtimeActualMs + (SystemClock.elapsedRealtime() - runtimeStartElapsedMs).coerceAtLeast(0)

    private suspend fun stopRequested(sessionId: String?) {
        val active = repository.activeSession()
        if (active == null) {
            stopSelf()
            return
        }
        // An old notification cannot stop a newer session.
        if (sessionId != active.id) {
            startOrRecover(null)
            return
        }
        timer?.cancel()
        repository.checkpoint(
            FocusSessionMachine.end(
                active,
                FocusStatus.CANCELLED,
                if (runtimeSession?.id == active.id) actualNow() else active.actualDurationMs,
                System.currentTimeMillis()
            )
        )
        runtimeSession = null
        stopSelf()
    }

    private fun notification(session: FocusSessionState?): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_focus_notification)
            .setContentTitle(getString(R.string.focus_notification_title))
            .setContentText(
                if (session == null) {
                    getString(R.string.focus_preparing)
                } else {
                    getString(R.string.focus_remaining_minutes, (session.remainingDurationMs + 59_999) / 60_000)
                }
            )
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        if (session != null) {
            val stop = PendingIntent.getService(
                this,
                session.id.hashCode(),
                Intent(this, FocusTimerService::class.java).apply {
                    action = ACTION_STOP
                    putExtra(EXTRA_SESSION, session.id)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, getString(R.string.focus_stop), stop)
        }
        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START: String = "com.screengarden.app.focus.START"
        const val ACTION_STOP: String = "com.screengarden.app.focus.STOP"
        const val EXTRA_MINUTES: String = "minutes"
        const val EXTRA_TAG: String = "tag"
        const val EXTRA_PLANT: String = "plant"
        const val EXTRA_SESSION: String = "session"
        private const val CHANNEL = "focus_timer"
        private const val NOTIFICATION = 101
    }
}
