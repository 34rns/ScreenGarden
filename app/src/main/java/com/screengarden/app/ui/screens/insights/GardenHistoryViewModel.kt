// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screengarden.app.domain.insights.GardenHistory
import com.screengarden.app.domain.insights.GardenHistoryEntry
import com.screengarden.app.domain.insights.GardenHistorySnapshot
import com.screengarden.app.domain.insights.GardenHistoryStore
import com.screengarden.app.domain.insights.HistoryPeriod
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GardenHistoryUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val period: HistoryPeriod = HistoryPeriod.WEEK,
    val entries: List<GardenHistoryEntry> = emptyList(),
    val unreadableFrames: Int = 0
)

/** Period changes reuse saved frames; retry restarts a failed observer without touching garden writes. */
class GardenHistoryViewModel(
    private val store: GardenHistoryStore,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val state = MutableStateFlow(GardenHistoryUiState())
    val uiState: StateFlow<GardenHistoryUiState> = state
    private val snapshot = MutableStateFlow(GardenHistorySnapshot())
    private var observeJob: Job? = null

    init {
        refresh()
    }

    fun setPeriod(period: HistoryPeriod) {
        state.update { it.copy(period = period, entries = GardenHistory.group(snapshot.value.frames, period, zone)) }
    }

    fun refresh() {
        observeJob?.cancel()
        state.update { it.copy(loading = true, error = false) }
        observeJob = viewModelScope.launch(dispatcher) {
            try {
                store.observe().collect { value ->
                    snapshot.value = value
                    state.update {
                        it.copy(
                            loading = false,
                            error = false,
                            entries = GardenHistory.group(value.frames, it.period, zone),
                            unreadableFrames = value.unreadableFrames
                        )
                    }
                }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                state.update { it.copy(loading = false, error = true) }
            }
        }
    }
}
