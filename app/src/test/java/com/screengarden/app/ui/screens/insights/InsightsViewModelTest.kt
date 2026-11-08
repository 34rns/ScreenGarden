// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.insights

import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.insights.HourlyDay
import com.screengarden.app.domain.insights.InsightsPeriod
import com.screengarden.app.domain.insights.InsightsSnapshot
import com.screengarden.app.domain.insights.InsightsStore
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val zone = ZoneId.of("UTC")
    private val now = 1_794_000_000_000L
    private var reads = 0
    private val store = object : InsightsStore {
        override fun observe(fromDay: Long, toDay: Long) = flowOf(InsightsSnapshot(emptyList(), emptyList()))
        override suspend fun recentHours(fromDay: Long, toDay: Long): List<HourlyDay> {
            reads++
            return listOf(HourlyDay(LocalDay.startOfDay(now, zone), List(24) { 0L }))
        }
    }

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun teardown() {
        Dispatchers.resetMain()
    }
    private fun model(access: Boolean = true, sync: suspend () -> Unit = {}) = InsightsViewModel(
        store,
        flowOf(10_800_000L),
        flowOf(80),
        { access },
        sync,
        { now },
        zone,
        dispatcher
    )

    @Test fun periodChangesKeepStateAndDoNotRequeryHourlyEvents() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        assertFalse(model.uiState.value.loading)
        assertEquals(7, model.uiState.value.summary.days.size)
        model.setPeriod(InsightsPeriod.MONTH)
        advanceUntilIdle()
        assertEquals(30, model.uiState.value.summary.days.size)
        assertEquals(1, reads)
    }

    @Test fun accessDeniedHasNoHourlyReadAndSavedDataIsStillObserved() = runTest(dispatcher) {
        val model = model(false)
        advanceUntilIdle()
        assertFalse(model.uiState.value.hasUsageAccess)
        assertEquals(0, reads)
        assertFalse(model.uiState.value.loading)
    }

    @Test fun refreshFailureIsVisibleAndAnotherRefreshCanRecover() = runTest(dispatcher) {
        var broken = true
        val model = model(sync = { if (broken) error("test read failed") })
        advanceUntilIdle()
        assertTrue(model.uiState.value.refreshError)
        broken = false
        model.refresh()
        advanceUntilIdle()
        assertFalse(model.uiState.value.refreshError)
        assertEquals(1, reads)
    }

    @Test fun aFailedHistoryObserverCanBeRestartedWithRefresh() = runTest(dispatcher) {
        var broken = true
        val failing = object : InsightsStore {
            override fun observe(fromDay: Long, toDay: Long) = flow {
                if (broken) error("test history unavailable")
                emit(InsightsSnapshot(emptyList(), emptyList()))
            }
            override suspend fun recentHours(fromDay: Long, toDay: Long) = emptyList<HourlyDay>()
        }
        val model = InsightsViewModel(
            failing,
            flowOf(10_800_000L),
            flowOf(80),
            { true },
            {},
            { now },
            zone,
            dispatcher
        )
        advanceUntilIdle()
        assertTrue(model.uiState.value.error)
        broken = false
        model.refresh()
        advanceUntilIdle()
        assertFalse(model.uiState.value.error)
    }
}
