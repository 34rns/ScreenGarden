// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.habits

import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.habits.CompassionResult
import com.screengarden.app.domain.habits.HabitsSnapshot
import com.screengarden.app.domain.habits.HabitsStore
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class HabitsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val date = LocalDate.of(2026, 11, 8)
    private val zone = ZoneId.of("UTC")

    @Before fun main() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun reset() {
        Dispatchers.resetMain()
    }

    @Test fun invalidInputDoesNotReachStorageAndQuotaFailuresStayVisible() = runTest(dispatcher) {
        val store = FakeStore()
        val model = model(store)
        advanceUntilIdle()
        model.setCompassionDay("not-a-date", true)
        assertEquals(HabitsMessage.INVALID_DATE, model.uiState.value.message)
        assertEquals(0, store.writes)
        store.result = CompassionResult.LIMIT_REACHED
        model.setCompassionDay(date.toString(), true)
        advanceUntilIdle()
        assertEquals(1, store.writes)
        assertEquals(HabitsMessage.LIMIT_REACHED, model.uiState.value.message)
    }

    @Test fun writeFailureAndRepeatedTapDoNotFabricateASuccess() = runTest(dispatcher) {
        val store = FakeStore().also { it.fail = true }
        val model = model(store)
        advanceUntilIdle()
        model.setCompassionDay(date.toString(), true)
        model.setCompassionDay(date.toString(), true)
        advanceUntilIdle()
        assertEquals(1, store.writes)
        assertEquals(HabitsMessage.SAVE_ERROR, model.uiState.value.message)
        assertEquals(false, model.uiState.value.saving)
    }

    @Test fun thresholdAndAllowanceUpdatesRoundTripThroughFlows() = runTest(dispatcher) {
        val model = model(FakeStore())
        advanceUntilIdle()
        model.setThreshold(70)
        advanceUntilIdle()
        model.setMonthlyLimit(5)
        advanceUntilIdle()
        assertEquals(70, model.uiState.value.threshold)
        assertEquals(5, model.uiState.value.monthlyLimit)
    }

    private fun model(store: FakeStore): HabitsViewModel {
        val threshold = MutableStateFlow(60)
        val limit = MutableStateFlow(3)
        return HabitsViewModel(
            store,
            threshold,
            limit,
            { threshold.value = it },
            { limit.value = it },
            { LocalDay.startOfDay(date, zone) },
            zone,
            dispatcher
        )
    }
    private class FakeStore : HabitsStore {
        var result = CompassionResult.SAVED
        var writes = 0
        var fail = false
        override fun observe() = MutableStateFlow(HabitsSnapshot(emptyList(), emptyList(), emptyList()))
        override suspend fun evaluateBadges(threshold: Int) { }
        override suspend fun setCompassionDay(date: LocalDate, enabled: Boolean, monthlyLimit: Int): CompassionResult {
            writes++
            if (fail) error("read-only storage")
            return result
        }
    }
}
