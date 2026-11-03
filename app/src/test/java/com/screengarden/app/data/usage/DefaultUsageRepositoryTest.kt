// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.usage

import com.screengarden.app.core.time.LocalDay
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for the aggregation rules, with a fake [UsageSource].
 *
 * The Android adapter is deliberately not covered here: it is a thin mapping over
 * UsageStatsManager, and faking the framework would test the fake. What must be
 * right is the arithmetic - windows, clipping, pairing and filters - and that is
 * all in [DefaultUsageRepository].
 *
 * Every call passes [ZONE] explicitly. Relying on the default system zone would
 * make the day window depend on the machine running the tests, which is exactly
 * the class of bug H13 exists to prevent.
 */
class DefaultUsageRepositoryTest {

    @Test
    fun missingPermissionIsAStateAndNoStatsAreQueried() {
        val source = FakeUsageSource(granted = false)

        val result = runBlocking { repository(source).readDay(DAY_START, ZONE) }

        assertEquals(DayUsageResult.MissingPermission, result)
        // The access check itself runs once; no usage data is read at all.
        assertEquals(1, source.accessChecks)
        assertEquals(0, source.dataQueries)
    }

    @Test
    fun sumsForegroundTimePerAppAndInTheTotal() {
        val source = FakeUsageSource(
            events = listOf(
                resumed(APP_A, 10),
                paused(APP_A, 25),
                resumed(APP_B, 30),
                paused(APP_B, 45)
            )
        )

        val usage = runBlocking { repository(source).readDay(DAY_START, ZONE) }.requireUsage()

        assertEquals(15 * MINUTE, usage.app(APP_A)?.usageMillis)
        assertEquals(15 * MINUTE, usage.app(APP_B)?.usageMillis)
        assertEquals(30 * MINUTE, usage.totalScreenTimeMillis)
        assertEquals(listOf(APP_A, APP_B), usage.apps.map { it.packageName })
        assertEquals(1, usage.app(APP_A)?.launchCount)
    }

    @Test
    fun closesAnIntervalLeftOpenAtTheEndOfTheDay() {
        val source = FakeUsageSource(events = listOf(resumed(APP_A, 23 * 60 + 30)))

        val usage = runBlocking { repository(source).readDay(DAY_START, ZONE) }.requireUsage()

        // 30 minutes in a 24 hour day (2026-03-10 is not a DST day in Berlin).
        assertEquals(30 * MINUTE, usage.app(APP_A)?.usageMillis)
        assertEquals(30 * MINUTE, usage.totalScreenTimeMillis)
    }

    @Test
    fun todaysOpenIntervalStopsAtNowInsteadOfTheEndOfTheDay() {
        // Regression (K38): a window that ends in the future made an app left in
        // the foreground count every remaining hour of the day.
        val source = FakeUsageSource(events = listOf(resumed(APP_A, 10)))

        val usage = runBlocking { repository(source, now = DAY_START + 60 * MINUTE).readDay(DAY_START, ZONE) }
            .requireUsage()

        assertEquals(50 * MINUTE, usage.app(APP_A)?.usageMillis)
        assertEquals(50 * MINUTE, usage.totalScreenTimeMillis)
    }

    @Test
    fun ignoresEventsOutsideTheRequestedDay() {
        val source = FakeUsageSource(
            events = listOf(
                resumed(APP_A, -10),
                paused(APP_A, -5),
                resumed(APP_B, 60),
                paused(APP_B, 70),
                resumed(APP_C, 24 * 60)
            )
        )

        val usage = runBlocking { repository(source).readDay(DAY_START, ZONE) }.requireUsage()

        assertEquals(10 * MINUTE, usage.totalScreenTimeMillis)
        assertEquals(listOf(APP_B), usage.apps.map { it.packageName })
    }

    @Test
    fun excludesAppsWithoutALauncherIcon() {
        val source = FakeUsageSource(
            launchable = setOf(APP_A),
            events = listOf(
                resumed("com.android.systemui", 5),
                paused("com.android.systemui", 35),
                resumed(APP_A, 40),
                paused(APP_A, 50)
            )
        )

        val usage = runBlocking { repository(source).readDay(DAY_START, ZONE) }.requireUsage()

        assertEquals(listOf(APP_A), usage.apps.map { it.packageName })
        assertEquals(10 * MINUTE, usage.totalScreenTimeMillis)
        assertNull(usage.app("com.android.systemui"))
    }

    @Test
    fun countsPickupsAndUnlocks() {
        val source = FakeUsageSource(
            events = listOf(
                UsageEvent(UsageEventType.SCREEN_INTERACTIVE, DAY_START + 1 * MINUTE, null),
                UsageEvent(UsageEventType.KEYGUARD_HIDDEN, DAY_START + 2 * MINUTE, null),
                UsageEvent(UsageEventType.SCREEN_INTERACTIVE, DAY_START + 3 * MINUTE, null),
                UsageEvent(UsageEventType.KEYGUARD_HIDDEN, DAY_START + 4 * MINUTE, null)
            )
        )

        val usage = runBlocking { repository(source).readDay(DAY_START, ZONE) }.requireUsage()

        assertEquals(2, usage.pickups)
        assertEquals(UnlockCount.Supported(2), usage.unlocks)
    }

    @Test
    fun unlocksAreUnsupportedWhenThePlatformHasNoKeyguardEvent() {
        // API 26-27 send no KEYGUARD_HIDDEN event at all, so the honest answer is
        // "cannot count" rather than a fabricated 0 (D48).
        val source = FakeUsageSource(
            supportsUnlocks = false,
            events = listOf(
                UsageEvent(UsageEventType.SCREEN_INTERACTIVE, DAY_START + 1 * MINUTE, null),
                UsageEvent(UsageEventType.SCREEN_INTERACTIVE, DAY_START + 2 * MINUTE, null),
                resumed(APP_A, 10),
                paused(APP_A, 20)
            )
        )

        val usage = runBlocking { repository(source).readDay(DAY_START, ZONE) }.requireUsage()

        assertEquals(UnlockCount.Unsupported, usage.unlocks)
        // Pickups and durations are unaffected by the missing keyguard stream.
        assertEquals(2, usage.pickups)
        assertEquals(10 * MINUTE, usage.totalScreenTimeMillis)
    }

    @Test
    fun duplicateResumeAndOrphanPauseAreIgnored() {
        val source = FakeUsageSource(
            events = listOf(
                paused(APP_A, 1),
                resumed(APP_A, 10),
                resumed(APP_A, 12),
                paused(APP_A, 20)
            )
        )

        val usage = runBlocking { repository(source).readDay(DAY_START, ZONE) }.requireUsage()

        assertEquals(10 * MINUTE, usage.app(APP_A)?.usageMillis)
        // The duplicate resume is not a second launch.
        assertEquals(1, usage.app(APP_A)?.launchCount)
    }

    @Test
    fun switchingAppsClosesThePreviousInterval() {
        val source = FakeUsageSource(
            events = listOf(
                resumed(APP_A, 10),
                resumed(APP_B, 15),
                paused(APP_B, 20)
            )
        )

        val usage = runBlocking { repository(source).readDay(DAY_START, ZONE) }.requireUsage()

        assertEquals(5 * MINUTE, usage.app(APP_A)?.usageMillis)
        assertEquals(5 * MINUTE, usage.app(APP_B)?.usageMillis)
        assertEquals(10 * MINUTE, usage.totalScreenTimeMillis)
    }

    @Test
    fun lastUsedComesFromTheSystemAndFallsBackToTheLastResume() {
        val source = FakeUsageSource(
            totals = listOf(
                AppTotal(APP_A, lastTimeUsedMillis = DAY_START + 999 * MINUTE, totalTimeInForegroundMillis = 1)
            ),
            events = listOf(
                resumed(APP_A, 10),
                paused(APP_A, 20),
                resumed(APP_B, 30),
                paused(APP_B, 40)
            )
        )

        val usage = runBlocking { repository(source).readDay(DAY_START, ZONE) }.requireUsage()

        assertEquals(DAY_START + 999 * MINUTE, usage.app(APP_A)?.lastUsedUtcMillis)
        assertEquals(DAY_START + 30 * MINUTE, usage.app(APP_B)?.lastUsedUtcMillis)
        // "Last used" is a timestamp, so it must not leak into the duration.
        assertEquals(10 * MINUTE, usage.app(APP_A)?.usageMillis)
    }

    @Test
    fun anAppWithOnlySystemTotalsIsNotReported() {
        val source = FakeUsageSource(
            totals = listOf(
                AppTotal(
                    APP_B,
                    lastTimeUsedMillis = DAY_START + 5 * MINUTE,
                    totalTimeInForegroundMillis = 60 * MINUTE
                )
            ),
            events = listOf(
                resumed(APP_A, 10),
                paused(APP_A, 20)
            )
        )

        val usage = runBlocking { repository(source).readDay(DAY_START, ZONE) }.requireUsage()

        assertEquals(listOf(APP_A), usage.apps.map { it.packageName })
        assertNull(usage.app(APP_B))
    }

    private fun repository(source: UsageSource, now: Long = DAY_START + 24 * 60 * MINUTE): UsageRepository =
        DefaultUsageRepository(source) { now }

    private fun resumed(packageName: String, atMinutes: Int): UsageEvent =
        UsageEvent(UsageEventType.ACTIVITY_RESUMED, DAY_START + atMinutes * MINUTE, packageName)

    private fun paused(packageName: String, atMinutes: Int): UsageEvent =
        UsageEvent(UsageEventType.ACTIVITY_PAUSED, DAY_START + atMinutes * MINUTE, packageName)

    private fun DayUsageResult.requireUsage(): DayUsage {
        assertTrue("expected Available but was $this", this is DayUsageResult.Available)
        return (this as DayUsageResult.Available).usage
    }

    private class FakeUsageSource(
        override var supportsUnlocks: Boolean = true,
        var granted: Boolean = true,
        var launchable: Set<String> = setOf(APP_A, APP_B, APP_C),
        var totals: List<AppTotal> = emptyList(),
        var events: List<UsageEvent> = emptyList()
    ) : UsageSource {

        var accessChecks: Int = 0
            private set

        var dataQueries: Int = 0
            private set

        override fun hasUsageAccess(): Boolean {
            accessChecks++
            return granted
        }

        override fun launchablePackages(): Set<String> {
            dataQueries++
            return launchable
        }

        override fun appTotals(startMillis: Long, endMillis: Long): List<AppTotal> {
            dataQueries++
            return totals
        }

        override fun events(startMillis: Long, endMillis: Long): List<UsageEvent> {
            dataQueries++
            return events
        }
    }

    private companion object {
        const val MINUTE = 60_000L
        const val APP_A = "com.example.a"
        const val APP_B = "com.example.b"
        const val APP_C = "com.example.c"

        /** A real local day in a zone with DST, so the helper is exercised too. */
        val ZONE: ZoneId = ZoneId.of("Europe/Berlin")
        val DAY_START: Long = LocalDay.startOfDay(LocalDate.of(2026, 3, 10), ZONE)
    }
}
