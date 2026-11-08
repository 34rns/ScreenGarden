// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NudgePolicyTest {
    @Test fun thresholdsIncludeTheExactBoundary() {
        assertTrue(NudgePolicy.crossed(149, 300).isEmpty())
        assertEquals(listOf(Nudge(50)), NudgePolicy.crossed(150, 300))
        assertEquals(listOf(Nudge(50), Nudge(80)), NudgePolicy.crossed(240, 300))
        assertEquals(listOf(Nudge(50), Nudge(80), Nudge(100)), NudgePolicy.crossed(300, 300))
    }

    @Test fun appsHaveIndependentMilestoneKeys() {
        assertEquals(listOf(Nudge(50, "fake.reader")), NudgePolicy.crossed(150, 300, "fake.reader"))
        assertTrue(Nudge(50).key != Nudge(50, "fake.reader").key)
    }

    @Test fun missingBudgetsAndNegativeUsageNeverNudge() {
        assertTrue(NudgePolicy.crossed(100, 0).isEmpty())
        assertTrue(NudgePolicy.crossed(100, -1).isEmpty())
        assertTrue(NudgePolicy.crossed(-1, 100).isEmpty())
    }

    @Test fun hugeUsageDoesNotOverflowBeforeComparing() {
        assertEquals(listOf(Nudge(50), Nudge(80), Nudge(100)), NudgePolicy.crossed(Long.MAX_VALUE, 300))
        assertEquals(listOf(Nudge(50), Nudge(80), Nudge(100)), NudgePolicy.crossed(Long.MAX_VALUE, Long.MAX_VALUE))
    }
}
