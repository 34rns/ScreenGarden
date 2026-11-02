// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.data.local.dao.AppLimitDao
import com.screengarden.app.data.local.entity.AppLimit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppLimitDaoTest {

    private lateinit var database: ScreenGardenDatabase
    private lateinit var dao: AppLimitDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ScreenGardenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.appLimitDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryByPackage() = runTest {
        dao.upsert(limit(CHAT, dailyLimitMinutes = 30))

        val stored = dao.get(CHAT)

        assertEquals(30, stored?.dailyLimitMinutes)
        assertTrue(stored?.isEnabled == true)
        assertNull(dao.get(READER))
    }

    @Test
    fun observeEnabledSkipsDisabledLimits() = runTest {
        dao.upsert(limit(CHAT, dailyLimitMinutes = 30))
        dao.upsert(limit(READER, dailyLimitMinutes = 45, isEnabled = false))

        val enabled = dao.observeEnabled().first()
        val all = dao.observeAll().first()

        assertEquals(listOf(CHAT), enabled.map { it.packageName })
        assertEquals(2, all.size)
    }

    @Test
    fun updateChangesLimitAndToggle() = runTest {
        dao.upsert(limit(CHAT, dailyLimitMinutes = 30))
        val stored = requireNotNull(dao.get(CHAT))

        val rowsUpdated = dao.update(stored.copy(dailyLimitMinutes = 5, isEnabled = false))

        assertEquals(1, rowsUpdated)
        assertEquals(5, dao.get(CHAT)?.dailyLimitMinutes)
        assertFalse(dao.get(CHAT)?.isEnabled ?: true)
    }

    @Test
    fun deleteRemovesLimit() = runTest {
        dao.upsert(limit(CHAT, dailyLimitMinutes = 30))

        assertEquals(1, dao.delete(CHAT))

        assertNull(dao.get(CHAT))
        assertEquals(0, dao.observeAll().first().size)
    }

    private fun limit(packageName: String, dailyLimitMinutes: Int, isEnabled: Boolean = true): AppLimit = AppLimit(
        packageName = packageName,
        dailyLimitMinutes = dailyLimitMinutes,
        isEnabled = isEnabled,
        updatedAtUtcMillis = DAY_ONE
    )

    private companion object {
        const val DAY_ONE = 1_700_000_000_000L
        const val READER = "com.example.reader"
        const val CHAT = "com.example.chat"
    }
}
