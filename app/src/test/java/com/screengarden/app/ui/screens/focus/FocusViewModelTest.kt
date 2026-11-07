// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.focus

import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.focus.FocusSessionState
import com.screengarden.app.domain.focus.FocusStatus
import com.screengarden.app.domain.focus.FocusStore
import com.screengarden.app.domain.focus.FocusTag
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val zone = ZoneId.of("UTC")
    private val now = 1_794_000_000_000L
    private val day = LocalDay.startOfDay(now, zone)
    private val sessions = MutableStateFlow(emptyList<FocusSessionState>())

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun model(start: (Int, FocusTag) -> Unit = { _, _ -> }): FocusViewModel = FocusViewModel(
        FocusStore { _, _ -> sessions },
        flowOf(false),
        start,
        { id ->
            sessions.value = sessions.value.map {
                if (it.id == id) {
                    it.copy(
                        status = FocusStatus.CANCELLED,
                        endedAtUtcMillis = now
                    )
                } else {
                    it
                }
            }
        },
        { },
        { now },
        zone,
        dispatcher
    )

    @Test
    fun timerStartAndStopFollowPersistedStateAndUseSelectedValues() = runTest(dispatcher) {
        val model = model { minutes, tag -> sessions.value = listOf(FocusSessionState("run", day, now, minutes, tag)) }
        advanceUntilIdle()
        model.start(45, FocusTag.WORK)
        advanceUntilIdle()
        assertEquals(45, model.uiState.value.active?.plannedMinutes)
        assertEquals(FocusTag.WORK, model.uiState.value.active?.tag)
        assertFalse(model.uiState.value.pending)
        model.stop()
        advanceUntilIdle()
        assertNull(model.uiState.value.active)
        assertEquals(FocusStatus.CANCELLED, model.uiState.value.result?.status)
    }

    @Test
    fun dismissingTheLatestResultDoesNotRedisplayOlderSessions() = runTest(dispatcher) {
        sessions.value = listOf(
            FocusSessionState(
                "old",
                day,
                now - 120_000,
                1,
                status = FocusStatus.COMPLETED,
                endedAtUtcMillis = now - 60_000,
                actualDurationMs = 60_000
            ),
            FocusSessionState(
                "new",
                day,
                now - 60_000,
                1,
                status = FocusStatus.COMPLETED,
                endedAtUtcMillis = now,
                actualDurationMs = 60_000
            )
        )
        val model = model()
        advanceUntilIdle()
        assertEquals(2, model.uiState.value.sessionsToday)
        assertEquals("new", model.uiState.value.result?.id)
        model.dismissResult()
        advanceUntilIdle()
        assertNull(model.uiState.value.result)
    }

    @Test
    fun aServiceThatDoesNotStartShowsAnActionError() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        model.start(25, FocusTag.STUDY)
        advanceUntilIdle()
        assertTrue(model.uiState.value.error)
        assertFalse(model.uiState.value.pending)
    }
}
