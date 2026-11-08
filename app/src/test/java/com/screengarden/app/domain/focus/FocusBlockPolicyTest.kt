// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.focus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusBlockPolicyTest {
    @Test fun blockingNeedsAnOptInActiveSessionAndSelectedPackage() {
        val policy = FocusBlockPolicy(blocked = setOf("demo.video"))
        assertFalse(policy.shouldRedirect("demo.video", true, emptySet()))
        assertFalse(policy.copy(enabled = true).shouldRedirect("demo.video", false, emptySet()))
        assertFalse(policy.copy(enabled = true).shouldRedirect(null, true, emptySet()))
        assertFalse(policy.copy(enabled = true).shouldRedirect("demo.notes", true, emptySet()))
        assertTrue(policy.copy(enabled = true).shouldRedirect("demo.video", true, emptySet()))
    }

    @Test fun allowListAndEscapeRoutesAlwaysWin() {
        val policy = FocusBlockPolicy(true, setOf("demo.video", "demo.phone"), setOf("demo.video"))
        assertFalse(policy.shouldRedirect("demo.video", true, emptySet()))
        assertFalse(policy.shouldRedirect("demo.phone", true, setOf("demo.phone")))
    }

    @Test fun staleClockRebootAndTerminalTimersNeverRestrictApps() {
        val timer = FocusSessionState("fake", 0, 1000, 25, updatedAtUtcMillis = 2000, bootCount = 3)
        assertTrue(hasFreshFocus(listOf(timer), 7000, 3))
        assertFalse(hasFreshFocus(listOf(timer), 7001, 3))
        assertFalse(hasFreshFocus(listOf(timer), 1999, 3))
        assertFalse(hasFreshFocus(listOf(timer), 3000, 4))
        assertFalse(hasFreshFocus(listOf(timer.copy(bootCount = -1)), 3000, -1))
        assertFalse(hasFreshFocus(listOf(timer.copy(status = FocusStatus.CANCELLED)), 3000, 3))
        assertFalse(hasFreshFocus(listOf(timer.copy(actualDurationMs = timer.plannedDurationMs)), 3000, 3))
    }

    @Test fun malformedAndUnboundedListsAreRejected() {
        for (name in listOf("", "../app", "com.demo\nlog", "com.")) {
            assertThrows(IllegalArgumentException::class.java) { FocusBlockPolicy(blocked = setOf(name)) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            FocusBlockPolicy(blocked = (0..256).map { "demo.app$it" }.toSet())
        }
    }
}
