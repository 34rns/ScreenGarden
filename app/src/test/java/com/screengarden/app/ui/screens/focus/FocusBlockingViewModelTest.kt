// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.focus

import com.screengarden.app.domain.focus.FocusBlockPolicy
import com.screengarden.app.domain.focus.FocusBlockStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class FocusBlockingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun finish() = Dispatchers.resetMain()

    @Test fun failedReadCannotOverwriteChoicesAndRetryRecovers() = runTest(dispatcher) {
        val store = Store()
        store.failRead = true
        val model = FocusBlockingViewModel(store, { emptyList() }, dispatcher)
        advanceUntilIdle()
        assertTrue(model.uiState.value.error)
        model.save(FocusBlockPolicy(true))
        advanceUntilIdle()
        assertEquals(0, store.writes)
        store.failRead = false
        model.refresh()
        advanceUntilIdle()
        assertFalse(model.uiState.value.error)
        model.save(FocusBlockPolicy(true, setOf("fake.video")))
        advanceUntilIdle()
        assertTrue(model.uiState.value.policy.enabled)
        assertEquals(1, store.writes)
    }

    @Test fun failedWriteKeepsSavedPolicyAndRequiresRetry() = runTest(dispatcher) {
        val store = Store()
        val model = FocusBlockingViewModel(store, { emptyList() }, dispatcher)
        advanceUntilIdle()
        store.failWrite = true
        model.save(FocusBlockPolicy(true))
        advanceUntilIdle()
        assertTrue(model.uiState.value.error)
        assertFalse(model.uiState.value.policy.enabled)
        assertFalse(model.uiState.value.saving)
    }

    private class Store : FocusBlockStore {
        val policy = MutableStateFlow(FocusBlockPolicy())
        var failRead = false
        var failWrite = false
        var writes = 0
        override fun observePolicy(): Flow<FocusBlockPolicy> = if (failRead) flow { error("fake read") } else policy
        override suspend fun save(policy: FocusBlockPolicy) {
            if (failWrite) error("fake write")
            writes++
            this.policy.value = policy
        }
    }
}
