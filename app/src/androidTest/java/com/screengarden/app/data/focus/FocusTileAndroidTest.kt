// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.focus

import android.content.ComponentName
import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.screengarden.app.MainActivity
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.domain.focus.FocusStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/** User-like SystemUI taps on the dedicated emulator; all session inputs are fake. */
@RunWith(AndroidJUnit4::class)
class FocusTileAndroidTest {
    @Test fun systemTileStartsTwentyFiveMinutesAndStopsTheSameSession() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = (context.applicationContext as ScreenGardenApplication).container
        val component = ComponentName(context, FocusTileService::class.java).flattenToString()
        val service = context.packageManager.getServiceInfo(ComponentName(context, FocusTileService::class.java), 0)
        assertEquals("android.permission.BIND_QUICK_SETTINGS_TILE", service.permission)
        container.database.focusSessionDao().deleteAll()
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).putExtra("screen", "focus_demo")
        ).use {
            try {
                shell("cmd statusbar add-tile $component")
                shell("cmd statusbar expand-settings")
                delay(2_000)
                shell("cmd statusbar click-tile $component")
                await { container.focusSessionRepository.activeSession() != null }
                val session = requireNotNull(container.focusSessionRepository.activeSession())
                assertEquals(25, session.plannedMinutes)
                assertNotNull(session.id)
                shell("cmd statusbar click-tile $component")
                await { container.focusSessionRepository.activeSession() == null }
                assertEquals(FocusStatus.CANCELLED, container.focusSessionRepository.getSession(session.id)?.status)
            } finally {
                container.focusSessionRepository.activeSession()?.let { container.focusController.stop(it.id) }
                shell("cmd statusbar remove-tile $component")
                shell("cmd statusbar collapse")
            }
        }
    }

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ).use { it.readBytes() }
    }

    private suspend fun await(condition: suspend () -> Boolean) {
        repeat(100) {
            if (condition()) return
            delay(100)
        }
        error("Focus tile did not update the persisted session")
    }
}
