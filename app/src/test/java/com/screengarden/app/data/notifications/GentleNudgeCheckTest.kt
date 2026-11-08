// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.notifications

import com.screengarden.app.core.time.BedtimeWindow
import com.screengarden.app.data.usage.AppUsage
import com.screengarden.app.data.usage.DayUsage
import com.screengarden.app.data.usage.DayUsageResult
import com.screengarden.app.data.usage.UnlockCount
import com.screengarden.app.domain.limits.AppLimitRule
import com.screengarden.app.domain.notifications.Nudge
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GentleNudgeCheckTest {
    @Test fun eachMilestoneAppearsOnceAcrossRepeatedChecks() = runTest {
        val fixture = Fixture()
        for (usage in listOf(500L, 500L, 800L, 800L, 1000L, 1000L)) {
            fixture.used = usage
            fixture.check.run()
        }
        assertEquals(listOf(Nudge(50), Nudge(80), Nudge(100)), fixture.posted)
    }

    @Test fun aLateCheckShowsOnlyTheHighestMilestone() = runTest {
        val fixture = Fixture()
        assertEquals(1, fixture.check.run())
        fixture.used = 600
        assertEquals(0, fixture.check.run())
        assertEquals(listOf(Nudge(100)), fixture.posted)
    }

    @Test fun offQuietAndDeniedChecksDoNotReadUsage() = runTest {
        val fixture = Fixture()
        fixture.allowUsage = false
        fixture.enabled = false
        assertEquals(0, fixture.check.run())
        fixture.enabled = true
        fixture.now = Instant.parse("2026-11-08T23:00:00Z").toEpochMilli()
        assertEquals(0, fixture.check.run())
        fixture.now = NOON
        fixture.allowed = false
        assertEquals(0, fixture.check.run())
        assertTrue(fixture.posted.isEmpty())
    }

    @Test fun missingAndUnboundedUsageCannotSendOrClaim() = runTest {
        val fixture = Fixture()
        fixture.missing = true
        assertEquals(0, fixture.check.run())
        fixture.missing = false
        fixture.bounded = false
        assertEquals(0, fixture.check.run())
        assertTrue(fixture.flags.isEmpty())
    }

    @Test fun enabledAppsAndDailyGoalHaveIndependentDelivery() = runTest {
        val fixture = Fixture()
        fixture.limits = listOf(AppLimitRule("fake.reader", 1), AppLimitRule("fake.paused", 1, false))
        fixture.apps = listOf(
            AppUsage("fake.reader", 30_000, 1, NOON),
            AppUsage("fake.paused", 100_000, 1, NOON)
        )
        assertEquals(2, fixture.check.run())
        assertEquals(listOf(Nudge(100), Nudge(50, "fake.reader")), fixture.posted)
        assertEquals(0, fixture.check.run())
    }

    @Test fun aBlockedChannelDoesNotConsumeItsMilestone() = runTest {
        val fixture = Fixture()
        fixture.channelAllowed = false
        assertEquals(0, fixture.check.run())
        assertTrue(fixture.flags.isEmpty())
        fixture.channelAllowed = true
        assertEquals(1, fixture.check.run())
    }

    @Test fun deliveryResetsOnTheNextLocalDay() = runTest {
        val fixture = Fixture()
        fixture.check.run()
        fixture.now = Instant.parse("2026-11-09T12:00:00Z").toEpochMilli()
        fixture.check.run()
        assertEquals(listOf(Nudge(100), Nudge(100)), fixture.posted)
    }

    @Test fun quietHoursStartingDuringTheReadSuppressDelivery() = runTest {
        val fixture = Fixture()
        fixture.afterRead = { fixture.now = Instant.parse("2026-11-08T22:30:00Z").toEpochMilli() }
        assertEquals(0, fixture.check.run())
        assertTrue(fixture.flags.isEmpty())
    }

    @Test fun aDayRolloverDuringTheReadCannotPostYesterdaysUsage() = runTest {
        val fixture = Fixture()
        fixture.afterRead = { fixture.now = Instant.parse("2026-11-09T12:00:00Z").toEpochMilli() }
        assertEquals(0, fixture.check.run())
        assertTrue(fixture.flags.isEmpty())
    }

    @Test fun concurrentChecksDoNotDuplicateNotifications() = runTest {
        val fixture = Fixture()
        val counts = listOf(async { fixture.check.run() }, async { fixture.check.run() }).awaitAll()
        assertEquals(1, counts.sum())
        assertEquals(listOf(Nudge(100)), fixture.posted)
    }

    private class Fixture {
        var now = NOON
        var used = 1000L
        var enabled = true
        var allowed = true
        var channelAllowed = true
        var allowUsage = true
        var missing = false
        var bounded = true
        var apps = emptyList<AppUsage>()
        var limits = emptyList<AppLimitRule>()
        var afterRead: () -> Unit = { }
        val posted = mutableListOf<Nudge>()
        val flags = mutableMapOf<Long, Set<String>>()
        val check = GentleNudgeCheck(
            preferences = {
                NudgePreferences(enabled, 1000, BedtimeWindow(LocalTime.of(22, 30), LocalTime.of(7, 0)))
            },
            readUsage = { day ->
                check(allowUsage) { "Usage must not be read when reminders are unavailable" }
                val result = if (missing) {
                    DayUsageResult.MissingPermission
                } else {
                    DayUsageResult.Available(
                        DayUsage(day, used, 0, UnlockCount.Unsupported, apps, bounded)
                    )
                }
                afterRead()
                result
            },
            readLimits = { limits },
            ledger = object : NudgeLedger {
                override suspend fun delivered(dayStartUtcMillis: Long) = flags[dayStartUtcMillis].orEmpty()
                override suspend fun claim(dayStartUtcMillis: Long, keys: Set<String>): Boolean {
                    val old = flags[dayStartUtcMillis].orEmpty()
                    if (keys.all { it in old }) return false
                    flags[dayStartUtcMillis] = old + keys
                    return true
                }
            },
            publisher = object : NudgePublisher {
                override fun canPost(nudge: Nudge?) = allowed && (nudge == null || channelAllowed)
                override fun post(nudge: Nudge): Boolean {
                    posted += nudge
                    return true
                }
            },
            nowMillis = { now },
            zoneId = { ZoneOffset.UTC }
        )
    }

    private companion object {
        val NOON: Long = Instant.parse("2026-11-08T12:00:00Z").toEpochMilli()
    }
}
