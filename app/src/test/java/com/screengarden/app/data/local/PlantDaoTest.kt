// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.data.local.dao.PlantDao
import com.screengarden.app.data.local.entity.Plant
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
class PlantDaoTest {

    private lateinit var database: ScreenGardenDatabase
    private lateinit var dao: PlantDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ScreenGardenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.plantDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryById() = runTest {
        dao.upsert(plant(PLANT_ONE, plantedAtUtcMillis = DAY_ONE))

        val stored = dao.getById(PLANT_ONE)

        assertEquals(PLANT_ONE, stored?.plantId)
        assertEquals("fern", stored?.species)
        assertEquals(0, stored?.stage)
        assertNull(stored?.lastFocusAtUtcMillis)
    }

    @Test
    fun observeAllOrdersByPlantingTime() = runTest {
        dao.upsert(plant(PLANT_TWO, plantedAtUtcMillis = DAY_TWO))
        dao.upsert(plant(PLANT_ONE, plantedAtUtcMillis = DAY_ONE))

        val plants = dao.observeAll().first()

        assertEquals(listOf(PLANT_ONE, PLANT_TWO), plants.map { it.plantId })
        assertEquals(2, dao.count())
    }

    @Test
    fun updateGrowsStageAndRecordsFocus() = runTest {
        dao.upsert(plant(PLANT_ONE, plantedAtUtcMillis = DAY_ONE))
        val stored = requireNotNull(dao.getById(PLANT_ONE))

        val rowsUpdated = dao.update(
            stored.copy(stage = 2, health = 80, lastFocusAtUtcMillis = DAY_TWO)
        )

        assertEquals(1, rowsUpdated)
        assertEquals(2, dao.getById(PLANT_ONE)?.stage)
        assertEquals(80, dao.getById(PLANT_ONE)?.health)
        assertEquals(DAY_TWO, dao.getById(PLANT_ONE)?.lastFocusAtUtcMillis)
    }

    @Test
    fun deleteRemovesPlant() = runTest {
        dao.upsert(plant(PLANT_ONE, plantedAtUtcMillis = DAY_ONE))

        assertEquals(1, dao.deleteById(PLANT_ONE))

        assertNull(dao.getById(PLANT_ONE))
        assertEquals(0, dao.count())
    }

    private fun plant(plantId: String, plantedAtUtcMillis: Long): Plant = Plant(
        plantId = plantId,
        species = "fern",
        plantedAtUtcMillis = plantedAtUtcMillis
    )

    private companion object {
        const val DAY_ONE = 1_700_000_000_000L
        const val DAY_TWO = 1_700_086_400_000L
        const val PLANT_ONE = "plant-1"
        const val PLANT_TWO = "plant-2"
    }
}
