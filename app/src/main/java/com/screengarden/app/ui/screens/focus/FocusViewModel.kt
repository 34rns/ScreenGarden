// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.focus.FocusSessionState
import com.screengarden.app.domain.focus.FocusStatus
import com.screengarden.app.domain.focus.FocusStore
import com.screengarden.app.domain.focus.FocusTag
import com.screengarden.app.domain.focus.summarizeFocus
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** One immutable state for setup, the persisted timer and its result. */
data class FocusUiState(
    val loading: Boolean = true,
    val pending: Boolean = false,
    val active: FocusSessionState? = null,
    val result: FocusSessionState? = null,
    val sessionsToday: Int = 0,
    val minutesThisWeek: Long = 0,
    val keepScreenOn: Boolean = false,
    val error: Boolean = false
)

/** Android starts and storage writes are injected at the composition root. */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModel(
    private val store: FocusStore,
    keepScreenOn: Flow<Boolean>,
    private val startTimer: (Int, FocusTag) -> Unit,
    private val stopTimer: (String) -> Unit,
    private val saveKeepScreenOn: suspend (Boolean) -> Unit,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val state = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = state
    private val refreshVersion = MutableStateFlow(0)
    private val dismissedResult = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch(dispatcher) {
            val sessions = refreshVersion.flatMapLatest {
                val today = LocalDay.startOfDay(nowMillis(), zone)
                var from = today
                repeat(7) { from = LocalDay.previousDayStart(from, zone) }
                store.observeSessions(from, today).catch { failure ->
                    if (failure is CancellationException) throw failure
                    state.update { it.copy(loading = false, error = true) }
                }
            }
            combine(sessions, keepScreenOn, dismissedResult) { rows, keep, dismissed ->
                val now = nowMillis()
                val today = LocalDay.startOfDay(now, zone)
                var week = today
                while (Instant.ofEpochMilli(week).atZone(zone).dayOfWeek != DayOfWeek.MONDAY) {
                    week = LocalDay.previousDayStart(week, zone)
                }
                val summary = summarizeFocus(rows, today, LocalDay.nextDayStart(today, zone), week, now)
                FocusUiState(
                    loading = false,
                    active = rows.firstOrNull { it.status == FocusStatus.RUNNING },
                    result = rows.filter { it.status != FocusStatus.RUNNING }
                        .filter { (it.endedAtUtcMillis ?: 0) in today..now }
                        .maxByOrNull { it.endedAtUtcMillis ?: 0 }?.takeIf { it.id != dismissed },
                    sessionsToday = summary.sessionsToday,
                    minutesThisWeek = summary.minutesThisWeek,
                    keepScreenOn = keep
                )
            }.catch { failure ->
                if (failure is CancellationException) throw failure
                state.update { it.copy(loading = false, error = true) }
            }.collect { observed ->
                state.update { observed.copy(pending = it.pending, error = it.error) }
            }
        }
    }

    fun refresh() {
        state.update { it.copy(error = false) }
        refreshVersion.update { it + 1 }
    }

    fun start(minutes: Int, tag: FocusTag) = command {
        require(minutes in 1..240)
        startTimer(minutes, tag)
        withTimeout(10_000) { uiState.first { it.active != null } }
    }

    fun stop() = command {
        state.value.active?.let { stopTimer(it.id) }
        withTimeout(10_000) { uiState.first { it.active == null } }
    }

    fun setKeepScreenOn(enabled: Boolean) = command { saveKeepScreenOn(enabled) }

    fun dismissResult() {
        dismissedResult.value = state.value.result?.id
    }

    private fun command(action: suspend () -> Unit) {
        if (state.value.pending) return
        state.update { it.copy(pending = true, error = false) }
        viewModelScope.launch(dispatcher) {
            try {
                action()
            } catch (failure: Exception) {
                if (failure is CancellationException && failure !is TimeoutCancellationException) throw failure
                state.update { it.copy(error = true) }
            } finally {
                state.update { it.copy(pending = false) }
            }
        }
    }
}
