// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.data.local.dao.FocusSessionDao
import com.screengarden.app.data.local.entity.FocusSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FocusSessionDaoTest {

    private lateinit var database: ScreenGardenDatabase
    private lateinit var dao: FocusSessionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ScreenGardenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.focusSessionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryById() = runTest {
        dao.upsert(session(SESSION_ONE, DAY_ONE, plannedMinutes = 25))

        val stored = dao.getById(SESSION_ONE)

        assertEquals(SESSION_ONE, stored?.sessionId)
        assertEquals(25, stored?.plannedMinutes)
        assertNull(stored?.endedAtUtcMillis)
    }

    @Test
    fun observeDayReturnsOnlyThatDay() = runTest {
        dao.upsert(session(SESSION_ONE, DAY_ONE))
        dao.upsert(session(SESSION_TWO, DAY_TWO))

        val todays = dao.sessionsForDay(DAY_ONE)

        assertEquals(listOf(SESSION_ONE), todays.map { it.sessionId })
    }

    @Test
    fun observeRunningFindsUnfinishedSession() = runTest {
        dao.upsert(session(SESSION_ONE, DAY_ONE))
        dao.upsert(session(SESSION_TWO, DAY_ONE, endedAtUtcMillis = DAY_ONE + 1_500_000))

        val running = dao.observeRunning().first()

        assertEquals(1, running.size)
        assertEquals(SESSION_ONE, running.single().sessionId)
    }

    @Test
    fun updateFinishesSession() = runTest {
        dao.upsert(session(SESSION_ONE, DAY_ONE, plannedMinutes = 25))
        val stored = requireNotNull(dao.getById(SESSION_ONE))

        val rowsUpdated = dao.update(
            stored.copy(endedAtUtcMillis = DAY_ONE + 1_500_000, wasCompleted = true)
        )

        assertEquals(1, rowsUpdated)
        assertTrue(dao.getById(SESSION_ONE)?.wasCompleted == true)
        assertEquals(DAY_ONE + 1_500_000, dao.getById(SESSION_ONE)?.endedAtUtcMillis)
        assertEquals(0, dao.observeRunning().first().size)
    }

    @Test
    fun deleteRemovesSession() = runTest {
        dao.upsert(session(SESSION_ONE, DAY_ONE))

        assertEquals(1, dao.deleteById(SESSION_ONE))

        assertNull(dao.getById(SESSION_ONE))
        assertEquals(0, dao.sessionsForDay(DAY_ONE).size)
    }

    private fun session(
        sessionId: String,
        dayStartUtcMillis: Long,
        plannedMinutes: Int = 25,
        endedAtUtcMillis: Long? = null
    ): FocusSession = FocusSession(
        sessionId = sessionId,
        dayStartUtcMillis = dayStartUtcMillis,
        startedAtUtcMillis = dayStartUtcMillis,
        endedAtUtcMillis = endedAtUtcMillis,
        plannedMinutes = plannedMinutes
    )

    private companion object {
        const val DAY_ONE = 1_700_000_000_000L
        const val DAY_TWO = 1_700_086_400_000L
        const val SESSION_ONE = "session-1"
        const val SESSION_TWO = "session-2"
    }
}
