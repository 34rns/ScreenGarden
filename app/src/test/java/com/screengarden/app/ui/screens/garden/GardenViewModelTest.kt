// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.garden.GardenDay
import com.screengarden.app.domain.garden.GardenDayRecord
import com.screengarden.app.domain.garden.GardenSnapshot
import com.screengarden.app.domain.garden.GardenStore
import com.screengarden.app.domain.garden.Plant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GardenViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val zone = ZoneId.of("UTC")
    private val today = LocalDay.startOfDay(1794000000000L, zone)
    private val yesterday = LocalDay.previousDayStart(today, zone)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun completedDaysAdvanceOnceAndTodayDoesNotAdvance() = runTest(dispatcher) {
        val store = FakeStore(listOf(day(yesterday), day(today)))
        val model = model(store)
        advanceUntilIdle()
        assertEquals(listOf(yesterday), store.applied.toList())
        assertEquals(79, model.uiState.value.score) // no completed focus is now a measured zero, not unknown
        model.refresh()
        advanceUntilIdle()
        assertEquals(1, store.applied.size)
    }

    @Test
    fun unboundedUsageHasNoScoreOrGardenReward() = runTest(dispatcher) {
        val store =
            FakeStore(listOf(day(yesterday).copy(usageIsBounded = false), day(today).copy(usageIsBounded = false)))
        val model = model(store)
        advanceUntilIdle()
        assertEquals(GardenAvailability.INCOMPLETE_USAGE, model.uiState.value.availability)
        assertEquals(null, model.uiState.value.score)
        assertEquals(emptySet<Long>(), store.applied)
    }

    @Test
    fun revokingPermissionRetainsSavedPlantsAndHidesScore() = runTest(dispatcher) {
        val store = FakeStore(listOf(day(today)))
        var granted = true
        val model = model(store) { granted }
        advanceUntilIdle()
        granted = false
        model.refresh()
        advanceUntilIdle()
        assertEquals(GardenAvailability.NO_PERMISSION, model.uiState.value.availability)
        assertEquals(null, model.uiState.value.score)
        assertEquals("saved", model.uiState.value.plants.single().id)
    }

    @Test
    fun syncFailureIsRecoverableAndKeepsSavedPlants() = runTest(dispatcher) {
        val store = FakeStore(listOf(day(today)))
        var fail = true
        val model = GardenViewModel(
            store, flowOf(10_800_000L), flowOf(80), flowOf(60), { true },
            { if (fail) error("test disk failure") }, { today + 60_000 }, zone, dispatcher
        )
        advanceUntilIdle()
        assertEquals(true, model.uiState.value.error)
        assertEquals("saved", model.uiState.value.plants.single().id)
        fail = false
        model.refresh()
        advanceUntilIdle()
        assertEquals(false, model.uiState.value.error)
    }

    private fun model(store: GardenStore, access: suspend () -> Boolean = { true }) = GardenViewModel(
        store, flowOf(10_800_000L), flowOf(80), flowOf(60), access, { }, { today + 60_000 }, zone, dispatcher
    )

    private fun day(key: Long) = GardenDayRecord(key, null, 30, 0, 0, 3, 1, true, false, 7L)

    private class FakeStore(days: List<GardenDayRecord>) : GardenStore {
        private val state = MutableStateFlow(GardenSnapshot(listOf(Plant("saved", "fern")), days))
        val applied = mutableSetOf<Long>()

        override fun observeSnapshot(fromDayStartUtcMillis: Long, toDayStartUtcMillis: Long): Flow<GardenSnapshot> =
            state

        override suspend fun recordScore(
            dayStartUtcMillis: Long,
            score: Int?,
            expectedUpdatedAtUtcMillis: Long
        ): Boolean {
            state.value = state.value.copy(
                days = state.value.days.map {
                    if (it.dayStartUtcMillis == dayStartUtcMillis) it.copy(score = score) else it
                }
            )
            return true
        }

        override suspend fun advanceCompletedDay(day: GardenDay): Boolean {
            if (!applied.add(day.dayStartUtcMillis)) return false
            state.value = state.value.copy(
                days = state.value.days.map {
                    if (it.dayStartUtcMillis == day.dayStartUtcMillis) it.copy(gardenApplied = true) else it
                }
            )
            return true
        }
    }
}
