// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.focus

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusSummaryTest {
    @Test
    fun overnightSessionCountsOnCompletionDayAndUnfinishedTimeEarnsNoTotals() {
        val sessions = listOf(
            FocusSessionState(
                "overnight",
                0,
                90_000,
                1,
                status = FocusStatus.COMPLETED,
                endedAtUtcMillis = 150_000,
                actualDurationMs = 60_000
            ),
            FocusSessionState(
                "cancel",
                100_000,
                150_000,
                1,
                status = FocusStatus.CANCELLED,
                endedAtUtcMillis = 175_000,
                actualDurationMs = 20_000
            ),
            FocusSessionState(
                "future",
                100_000,
                190_000,
                1,
                status = FocusStatus.COMPLETED,
                endedAtUtcMillis = 250_000,
                actualDurationMs = 60_000
            ),
            FocusSessionState(
                "old",
                0,
                0,
                1,
                status = FocusStatus.COMPLETED,
                endedAtUtcMillis = 60_000,
                actualDurationMs = 60_000
            )
        )
        assertEquals(FocusSummary(1, 2), summarizeFocus(sessions, 100_000, 200_000, 0, 190_000))
        assertEquals(FocusSummary(1, 1), summarizeFocus(sessions, 100_000, 200_000, 100_000, 190_000))
    }
}
