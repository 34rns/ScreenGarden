// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.data.local.dao.AppUsageDailyDao
import com.screengarden.app.data.local.entity.AppUsageDaily
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
class AppUsageDailyDaoTest {

    private lateinit var database: ScreenGardenDatabase
    private lateinit var dao: AppUsageDailyDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ScreenGardenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.appUsageDailyDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryByCompositeKey() = runTest {
        dao.upsert(usage(DAY_ONE, READER, usageMillis = 60_000))

        val stored = dao.get(DAY_ONE, READER)

        assertEquals(60_000L, stored?.usageMillis)
        assertNull(dao.get(DAY_TWO, READER))
    }

    @Test
    fun upsertAllKeepsOneRowPerDayAndApp() = runTest {
        dao.upsertAll(listOf(usage(DAY_ONE, READER, usageMillis = 1_000), usage(DAY_ONE, CHAT, 2_000)))

        dao.upsertAll(listOf(usage(DAY_ONE, READER, usageMillis = 9_000)))

        val rows = dao.observeDay(DAY_ONE).first()
        assertEquals(2, rows.size)
        assertEquals(9_000L, dao.get(DAY_ONE, READER)?.usageMillis)
    }

    @Test
    fun observeDayOrdersByUsageDescending() = runTest {
        dao.upsertAll(listOf(usage(DAY_ONE, READER, usageMillis = 1_000), usage(DAY_ONE, CHAT, 5_000)))

        val rows = dao.observeDay(DAY_ONE).first()

        assertEquals(listOf(CHAT, READER), rows.map { it.packageName })
    }

    @Test
    fun updateAndDelete() = runTest {
        dao.upsert(usage(DAY_ONE, READER, usageMillis = 1_000, launchCount = 1))
        val stored = requireNotNull(dao.get(DAY_ONE, READER))

        val rowsUpdated = dao.update(stored.copy(launchCount = 4))
        assertEquals(1, rowsUpdated)
        assertEquals(4, dao.get(DAY_ONE, READER)?.launchCount)

        assertEquals(1, dao.delete(DAY_ONE, READER))
        assertNull(dao.get(DAY_ONE, READER))
    }

    @Test
    fun deleteDayRemovesOnlyThatDay() = runTest {
        dao.upsertAll(
            listOf(
                usage(DAY_ONE, READER, usageMillis = 1_000),
                usage(DAY_TWO, READER, usageMillis = 2_000)
            )
        )

        assertEquals(1, dao.deleteDay(DAY_ONE))

        assertNull(dao.get(DAY_ONE, READER))
        assertEquals(2_000L, dao.get(DAY_TWO, READER)?.usageMillis)
    }

    private fun usage(
        dayStartUtcMillis: Long,
        packageName: String,
        usageMillis: Long = 0L,
        launchCount: Int = 0
    ): AppUsageDaily = AppUsageDaily(
        dayStartUtcMillis = dayStartUtcMillis,
        packageName = packageName,
        usageMillis = usageMillis,
        launchCount = launchCount,
        updatedAtUtcMillis = dayStartUtcMillis
    )

    private companion object {
        const val DAY_ONE = 1_700_000_000_000L
        const val DAY_TWO = 1_700_086_400_000L
        const val READER = "com.example.reader"
        const val CHAT = "com.example.chat"
    }
}
