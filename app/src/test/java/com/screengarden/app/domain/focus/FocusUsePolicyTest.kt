// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusUsePolicyTest {
    @Test fun lateWakeupsCannotCountPhoneUseAfterThePlannedEnd() {
        val session = FocusSessionState("fake", 0, 1_000, 1)
        assertEquals(61_000L, FocusUsePolicy.windowEndUtcMillis(session, 121_000, 120_000))
        assertEquals(31_000L, FocusUsePolicy.windowEndUtcMillis(session, 31_000, 30_000))
        assertNull(FocusUsePolicy.windowEndUtcMillis(session, 201_000, 120_000))
        assertNull(FocusUsePolicy.windowEndUtcMillis(session, 0, 30_000))
        val distant = session.copy(startedAtUtcMillis = Long.MAX_VALUE - 1)
        assertEquals(Long.MAX_VALUE, FocusUsePolicy.windowEndUtcMillis(distant, Long.MAX_VALUE, 5_000))
    }

    @Test fun briefChecksAndMissingEvidenceNeverFail() {
        for (used in listOf(null, -1L, 0L, 29_999L, 30_000L)) {
            assertFalse(FocusUsePolicy.shouldSoftFail(used))
        }
        assertTrue(FocusUsePolicy.shouldSoftFail(30_001))
        assertFalse(FocusUsePolicy.shouldSoftFail(60_000, graceMs = 60_000))
    }
}
