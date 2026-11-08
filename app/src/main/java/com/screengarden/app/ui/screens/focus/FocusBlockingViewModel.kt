// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screengarden.app.domain.focus.FocusBlockPolicy
import com.screengarden.app.domain.focus.FocusBlockStore
import com.screengarden.app.domain.limits.LaunchableApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocusBlockingUiState(
    val policy: FocusBlockPolicy = FocusBlockPolicy(),
    val apps: List<LaunchableApp> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: Boolean = false
)

/** Saved policy is authoritative; a failed write stays visible and never pretends blocking is on. */
class FocusBlockingViewModel(
    private val store: FocusBlockStore,
    private val loadApps: suspend () -> List<LaunchableApp>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val state = MutableStateFlow(FocusBlockingUiState())
    val uiState: StateFlow<FocusBlockingUiState> = state
    private var observer: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        observer?.cancel()
        observer = viewModelScope.launch(dispatcher) {
            state.update { it.copy(loading = true, error = false) }
            try {
                val apps = loadApps()
                state.update { it.copy(apps = apps) }
                store.observePolicy().collect { policy -> state.update { it.copy(policy = policy, loading = false) } }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                state.update { it.copy(error = true, loading = false) }
            }
        }
    }

    fun save(policy: FocusBlockPolicy) {
        if (state.value.loading || state.value.saving || state.value.error) return
        state.update { it.copy(saving = true, error = false) }
        viewModelScope.launch(dispatcher) {
            try {
                store.save(policy)
                state.update { it.copy(policy = policy) }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                state.update { it.copy(error = true) }
            } finally {
                state.update { it.copy(saving = false) }
            }
        }
    }
}
