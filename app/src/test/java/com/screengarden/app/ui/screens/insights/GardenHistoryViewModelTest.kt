// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.insights

import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.insights.GardenHistoryFrame
import com.screengarden.app.domain.insights.GardenHistorySnapshot
import com.screengarden.app.domain.insights.GardenHistoryStore
import com.screengarden.app.domain.insights.HistoryPeriod
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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
class GardenHistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val zone = ZoneId.of("UTC")

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun teardown() = Dispatchers.resetMain()

    @Test fun failedObserverRetriesAndPeriodsReuseActualFramesWithoutRereading() = runTest(dispatcher) {
        var broken = true
        var reads = 0
        val store = object : GardenHistoryStore {
            override fun observe() = flow {
                reads++
                if (broken) error("test history read failed")
                emit(
                    GardenHistorySnapshot(
                        listOf("2026-11-02", "2026-11-09").map {
                            GardenHistoryFrame(LocalDay.startOfDay(LocalDate.parse(it), zone), 1, 80, emptyList())
                        }
                    )
                )
            }
        }
        val model = GardenHistoryViewModel(store, zone, dispatcher)
        advanceUntilIdle()
        assertTrue(model.uiState.value.error)
        broken = false
        model.refresh()
        advanceUntilIdle()
        assertFalse(model.uiState.value.error)
        assertEquals(2, model.uiState.value.entries.size)
        model.setPeriod(HistoryPeriod.MONTH)
        assertEquals(1, model.uiState.value.entries.size)
        assertEquals(2, reads)
    }
}
