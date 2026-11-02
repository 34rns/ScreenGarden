// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.data.local.dao.BadgeDao
import com.screengarden.app.data.local.entity.Badge
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
class BadgeDaoTest {

    private lateinit var database: ScreenGardenDatabase
    private lateinit var dao: BadgeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ScreenGardenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.badgeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryByKey() = runTest {
        dao.upsert(Badge(badgeKey = FIRST_SPROUT, unlockedAtUtcMillis = DAY_ONE))

        val stored = dao.get(FIRST_SPROUT)

        assertEquals(FIRST_SPROUT, stored?.badgeKey)
        assertEquals(DAY_ONE, stored?.unlockedAtUtcMillis)
        assertNull(dao.get("not-earned"))
    }

    @Test
    fun observeAllReturnsNewestFirst() = runTest {
        dao.upsert(Badge(badgeKey = FIRST_SPROUT, unlockedAtUtcMillis = DAY_ONE))
        dao.upsert(Badge(badgeKey = WEEK_OF_CALM, unlockedAtUtcMillis = DAY_TWO))

        val badges = dao.observeAll().first()

        assertEquals(listOf(WEEK_OF_CALM, FIRST_SPROUT), badges.map { it.badgeKey })
        assertEquals(2, dao.count())
    }

    @Test
    fun updateChangesTimestamp() = runTest {
        dao.upsert(Badge(badgeKey = FIRST_SPROUT, unlockedAtUtcMillis = DAY_ONE))
        val stored = requireNotNull(dao.get(FIRST_SPROUT))

        val rowsUpdated = dao.update(stored.copy(unlockedAtUtcMillis = DAY_TWO))

        assertEquals(1, rowsUpdated)
        assertEquals(DAY_TWO, dao.get(FIRST_SPROUT)?.unlockedAtUtcMillis)
    }

    @Test
    fun deleteRemovesBadge() = runTest {
        dao.upsert(Badge(badgeKey = FIRST_SPROUT, unlockedAtUtcMillis = DAY_ONE))

        assertEquals(1, dao.delete(FIRST_SPROUT))

        assertNull(dao.get(FIRST_SPROUT))
        assertEquals(0, dao.count())
    }

    private companion object {
        const val DAY_ONE = 1_700_000_000_000L
        const val DAY_TWO = 1_700_086_400_000L
        const val FIRST_SPROUT = "first_sprout"
        const val WEEK_OF_CALM = "week_of_calm"
    }
}
