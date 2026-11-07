// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.domain.limits.AppLimitRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppLimitRepositoryImplTest {
    private lateinit var database: ScreenGardenDatabase
    private lateinit var repository: AppLimitRepositoryImpl

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ScreenGardenDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = AppLimitRepositoryImpl(database.appLimitDao()) { 100L }
    }

    @After fun tearDown() {
        database.close()
    }

    @Test fun pausingAndResumingKeepsTheDurationAndOneRow() = runTest {
        repository.saveLimit(AppLimitRule("fake.video", 30))
        repository.saveLimit(AppLimitRule("fake.video", 30, false))
        assertTrue(repository.observeEnabled().first().isEmpty())
        assertEquals(AppLimitRule("fake.video", 30, false), repository.observeLimits().first().single())
        assertEquals(100L, repository.get("fake.video")?.updatedAtUtcMillis)
        repository.saveLimit(AppLimitRule("fake.video", 30))
        assertEquals(1, repository.observeEnabled().first().size)
        repository.delete("fake.video")
        assertTrue(repository.observeLimits().first().isEmpty())
    }

    @Test fun invalidLimitsCannotEnterTheDatabase() = runTest {
        for (rule in listOf(AppLimitRule("", 30), AppLimitRule("fake", 0), AppLimitRule("fake", 1441))) {
            var rejected = false
            try {
                repository.saveLimit(rule)
            } catch (_: IllegalArgumentException) {
                rejected = true
            }
            assertTrue(rejected)
        }
        assertTrue(repository.observeAll().first().isEmpty())
    }
}
