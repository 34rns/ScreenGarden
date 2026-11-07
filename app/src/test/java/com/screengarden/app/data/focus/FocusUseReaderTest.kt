// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.focus

import com.screengarden.app.data.usage.AppTotal
import com.screengarden.app.data.usage.DefaultUsageRepository
import com.screengarden.app.data.usage.UsageEvent
import com.screengarden.app.data.usage.UsageEventType
import com.screengarden.app.data.usage.UsageSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusUseReaderTest {
    private class Source(
        val rows: List<UsageEvent>,
        var granted: Boolean = true,
        override val supportsScreenState: Boolean = true
    ) : UsageSource {
        override val supportsUnlocks: Boolean = true
        override fun hasUsageAccess(): Boolean = granted
        override fun launchablePackages(): Set<String> = setOf("self", "home", "other")
        override fun appTotals(startMillis: Long, endMillis: Long): List<AppTotal> = emptyList()
        override fun events(startMillis: Long, endMillis: Long): List<UsageEvent> = rows
    }

    private fun event(type: UsageEventType, time: Long, app: String? = null) = UsageEvent(type, time, app)
    private fun reader(source: Source, now: Long = 200_000) =
        FocusUseReader(DefaultUsageRepository(source) { now }, setOf("self", "home"))

    @Test fun onlyInteractiveOtherAppTimeInsideTheSessionCounts() {
        val source = Source(
            listOf(
                event(UsageEventType.ACTIVITY_RESUMED, 1_000, "other"),
                event(UsageEventType.ACTIVITY_RESUMED, 10_000, "self"),
                event(UsageEventType.ACTIVITY_RESUMED, 20_000, "home"),
                event(UsageEventType.ACTIVITY_RESUMED, 30_000, "other"),
                event(UsageEventType.SCREEN_NON_INTERACTIVE, 40_000),
                event(UsageEventType.SCREEN_INTERACTIVE, 140_000),
                event(UsageEventType.ACTIVITY_PAUSED, 160_000, "other")
            )
        )
        assertEquals(30_000L, reader(source).otherAppUsageMillis(10_000, 200_000))
    }

    @Test fun missingPermissionAndUnsupportedScreenEventsAreUnknown() {
        assertNull(reader(Source(emptyList(), granted = false)).otherAppUsageMillis(1, 10))
        assertNull(reader(Source(emptyList(), supportsScreenState = false)).otherAppUsageMillis(1, 10))
    }

    @Test fun ongoingUsageStopsAtNowAndRepeatedReadsDoNotAccumulate() {
        val source = Source(listOf(event(UsageEventType.ACTIVITY_RESUMED, 10_000, "other")))
        val reader = reader(source, now = 40_000)
        repeat(3) { assertEquals(30_000L, reader.otherAppUsageMillis(1, 200_000)) }
    }
}
