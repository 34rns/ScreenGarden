// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.limits

import com.screengarden.app.domain.limits.AppLimitRule
import com.screengarden.app.domain.limits.AppLimitStore
import com.screengarden.app.domain.limits.LaunchableApp
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppLimitsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun inventoryFailuresCanRetryAndPausingPreservesChoice() = runTest(dispatcher) {
        val store = Store()
        var failed = true
        val model = AppLimitsViewModel(store, {
            if (failed) error("fake inventory error")
            listOf(LaunchableApp("fake.video", "Videos"))
        }, dispatcher)
        advanceUntilIdle()
        assertTrue(model.uiState.value.error)
        failed = false
        model.refresh()
        advanceUntilIdle()
        assertFalse(model.uiState.value.error)
        model.save("fake.video", 45, false)
        advanceUntilIdle()
        assertEquals(AppLimitRule("fake.video", 45, false), model.uiState.value.limits.single())
        assertFalse(model.uiState.value.saving)
    }

    @Test fun failedWriteRetainsPreviousLimitAndShowsError() = runTest(dispatcher) {
        val store = Store()
        val model = AppLimitsViewModel(store, { emptyList() }, dispatcher)
        advanceUntilIdle()
        model.save("fake", 30, true)
        advanceUntilIdle()
        store.failWrites = true
        model.save("fake", 60, true)
        advanceUntilIdle()
        assertTrue(model.uiState.value.error)
        assertEquals(30, model.uiState.value.limits.single().dailyLimitMinutes)
        assertFalse(model.uiState.value.saving)
    }

    private class Store : AppLimitStore {
        val rows = MutableStateFlow<List<AppLimitRule>>(emptyList())
        var failWrites = false
        override fun observeLimits() = rows
        override suspend fun saveLimit(rule: AppLimitRule) {
            if (failWrites) error("fake write failure")
            rows.value = listOf(rule)
        }
    }
}
