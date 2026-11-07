// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.limits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screengarden.app.domain.limits.AppLimitRule
import com.screengarden.app.domain.limits.AppLimitStore
import com.screengarden.app.domain.limits.LaunchableApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppLimitsUiState(
    val apps: List<LaunchableApp> = emptyList(),
    val limits: List<AppLimitRule> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: Boolean = false
)

/** App inventory and storage run off the main thread; errors never pretend a limit was saved. */
class AppLimitsViewModel(
    private val store: AppLimitStore,
    private val loadApps: suspend () -> List<LaunchableApp>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val state = MutableStateFlow(AppLimitsUiState())
    val uiState: StateFlow<AppLimitsUiState> = state
    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(dispatcher) {
            state.update { it.copy(loading = true, error = false) }
            try {
                val apps = loadApps()
                state.update { it.copy(apps = apps, loading = false) }
                store.observeLimits().collect { limits -> state.update { it.copy(limits = limits) } }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                state.update { it.copy(error = true, loading = false) }
            }
        }
    }

    fun save(packageName: String, minutes: Int, enabled: Boolean) {
        if (state.value.saving || packageName.isBlank() || minutes !in 1..1440) return
        state.update { it.copy(saving = true, error = false) }
        viewModelScope.launch(dispatcher) {
            try {
                store.saveLimit(AppLimitRule(packageName, minutes, enabled))
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                state.update { it.copy(error = true) }
            } finally {
                state.update { it.copy(saving = false) }
            }
        }
    }
}
