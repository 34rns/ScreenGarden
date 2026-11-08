// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PreferencesNudgeLedgerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before fun resetFakeFlags() {
        context.getSharedPreferences("nudge_delivery_flags", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun deliveredFlagsSurviveANewInstanceAndStayDayScoped() = runTest {
        val first = PreferencesNudgeLedger(context)
        assertTrue(first.claim(1L, setOf("daily:50", "daily:80")))
        val restarted = PreferencesNudgeLedger(context)
        assertEquals(setOf("daily:50", "daily:80"), restarted.delivered(1L))
        assertFalse(restarted.claim(1L, setOf("daily:50")))
        assertTrue(restarted.claim(2L, setOf("daily:50")))
        assertEquals(setOf("daily:50", "daily:80"), restarted.delivered(1L))
    }

    @Test fun concurrentInstancesClaimTheSameMilestoneOnce() = runTest {
        val results = listOf(
            async { PreferencesNudgeLedger(context).claim(1L, setOf("daily:50")) },
            async { PreferencesNudgeLedger(context).claim(1L, setOf("daily:50")) }
        ).awaitAll()
        assertEquals(1, results.count { it })
    }
}
