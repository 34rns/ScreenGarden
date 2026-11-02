// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.data.local.dao.DailyStatDao
import com.screengarden.app.data.local.entity.DailyStat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DailyStatDaoTest {

    private lateinit var database: ScreenGardenDatabase
    private lateinit var dao: DailyStatDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ScreenGardenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.dailyStatDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryDay() = runTest {
        dao.upsert(stat(DAY_ONE, focusMinutes = 25))

        val stored = dao.getByDay(DAY_ONE)

        assertEquals(DAY_ONE, stored?.dayStartUtcMillis)
        assertEquals(25, stored?.focusMinutes)
    }

    @Test
    fun updateChangesValues() = runTest {
        dao.upsert(stat(DAY_ONE, focusMinutes = 25))
        val stored = requireNotNull(dao.getByDay(DAY_ONE))

        val rowsUpdated = dao.update(stored.copy(focusMinutes = 50, sessionsCompleted = 2))

        assertEquals(1, rowsUpdated)
        assertEquals(50, dao.getByDay(DAY_ONE)?.focusMinutes)
        assertEquals(2, dao.getByDay(DAY_ONE)?.sessionsCompleted)
    }

    @Test
    fun upsertSameDayDoesNotDuplicate() = runTest {
        dao.upsert(stat(DAY_ONE, focusMinutes = 10))
        dao.upsert(stat(DAY_ONE, focusMinutes = 30))

        val days = dao.observeRange(DAY_ONE, DAY_TWO).first()

        assertEquals(1, days.size)
        assertEquals(30, days.single().focusMinutes)
    }

    @Test
    fun observeRangeIsOrderedAndDeleteRemovesRow() = runTest {
        dao.upsert(stat(DAY_TWO, focusMinutes = 20))
        dao.upsert(stat(DAY_ONE, focusMinutes = 10))

        val days = dao.observeRange(DAY_ONE, DAY_TWO).first()
        assertEquals(listOf(DAY_ONE, DAY_TWO), days.map { it.dayStartUtcMillis })

        assertEquals(1, dao.deleteByDay(DAY_ONE))
        assertNull(dao.getByDay(DAY_ONE))
        assertEquals(1, dao.observeRange(DAY_ONE, DAY_TWO).first().size)
    }

    private fun stat(dayStartUtcMillis: Long, focusMinutes: Int = 0): DailyStat = DailyStat(
        dayStartUtcMillis = dayStartUtcMillis,
        focusMinutes = focusMinutes,
        updatedAtUtcMillis = dayStartUtcMillis
    )

    private companion object {
        const val DAY_ONE = 1_700_000_000_000L
        const val DAY_TWO = 1_700_086_400_000L
    }
}
