// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSessionMachineTest {
    private val session = FocusSessionState("session", 0, 1000, 1, bootCount = 3)

    @Test fun timerCompletesAtPlannedDurationAndKeepsActualBounded() {
        val running = FocusSessionMachine.tick(session, 59_999, 61_000)
        assertEquals(FocusStatus.RUNNING, running.status)
        assertNull(running.endedAtUtcMillis)
        val complete = FocusSessionMachine.tick(running, Long.MAX_VALUE, 62_000)
        assertEquals(FocusStatus.COMPLETED, complete.status)
        assertEquals(60_000L, complete.actualDurationMs)
        assertEquals(62_000L, complete.endedAtUtcMillis)
    }

    @Test fun progressCannotMoveBackward() {
        val progressed = FocusSessionMachine.tick(session, 30_000, 31_000)
        assertEquals(30_000L, FocusSessionMachine.tick(progressed, -1, 32_000).actualDurationMs)
    }

    @Test fun cancellationAndSoftFailureKeepTheEarnedDuration() {
        for (status in listOf(FocusStatus.CANCELLED, FocusStatus.FAILED_SOFT)) {
            val ended = FocusSessionMachine.end(session, status, 20_000, 21_000)
            assertEquals(status, ended.status)
            assertEquals(20_000L, ended.actualDurationMs)
            assertEquals(ended, FocusSessionMachine.tick(ended, 60_000, 90_000))
        }
    }

    @Test fun completedSessionsCannotBeCancelledOrRestarted() {
        val done = FocusSessionMachine.tick(session, 60_000, 61_000)
        assertEquals(done, FocusSessionMachine.end(done, FocusStatus.CANCELLED, 0, 80_000))
        assertEquals(done, FocusSessionMachine.recover(done, 0, 10))
    }

    @Test fun shortProcessGapResumesFromCheckpoint() {
        val checkpoint = FocusSessionMachine.tick(session, 10_000, 11_000)
        val resumed = FocusSessionMachine.recover(checkpoint, 16_000, 3)
        assertEquals(FocusStatus.RUNNING, resumed.status)
        assertEquals(15_000L, resumed.actualDurationMs)
    }

    @Test fun rebootClosesSessionWithoutAddingUnknownTime() {
        val checkpoint = FocusSessionMachine.tick(session, 10_000, 11_000)
        val ended = FocusSessionMachine.recover(checkpoint, 16_000, 4)
        assertEquals(FocusStatus.CANCELLED, ended.status)
        assertEquals(10_000L, ended.actualDurationMs)
    }

    @Test fun backwardClockOrOverdueGapDoesNotFabricateCompletion() {
        for (now in listOf(0L, 61_000L, Long.MAX_VALUE)) {
            val ended = FocusSessionMachine.recover(session, now, 3)
            assertEquals(FocusStatus.CANCELLED, ended.status)
            assertEquals(0L, ended.actualDurationMs)
        }
    }

    @Test fun everyTerminalStatusHasNoRemainingTimerWork() {
        for (status in FocusStatus.entries.filter { it != FocusStatus.RUNNING }) {
            val ended = session.copy(status = status)
            assertEquals(ended, FocusSessionMachine.tick(ended, 50_000, 51_000))
        }
    }

    @Test fun boundaryDurationsRemainSafe() {
        for (minutes in listOf(1, 240)) {
            val configured = session.copy(plannedMinutes = minutes)
            val end = FocusSessionMachine.tick(configured, Long.MAX_VALUE, 99_000)
            assertTrue(end.actualDurationMs <= minutes * 60_000L)
        }
    }
}
