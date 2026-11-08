// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.insights.HourlyDay
import com.screengarden.app.domain.insights.InsightSummary
import com.screengarden.app.domain.insights.InsightsPeriod
import com.screengarden.app.domain.insights.InsightsStore
import com.screengarden.app.domain.insights.InsightsSummary
import com.screengarden.app.domain.insights.Reflections
import com.screengarden.app.domain.insights.WeeklyReflection
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsightsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val period: InsightsPeriod = InsightsPeriod.WEEK,
    val goalMinutes: Int = 180,
    val summary: InsightSummary = InsightSummary(),
    val hours: List<HourlyDay> = emptyList(),
    val hasUsageAccess: Boolean = false,
    val error: Boolean = false,
    val refreshError: Boolean = false,
    val reflection: WeeklyReflection = WeeklyReflection()
)

/** One observable state; range toggles reuse measurements and refresh only on explicit/visible entry. */
@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(
    private val store: InsightsStore,
    dailyGoalMs: Flow<Long>,
    pickupGoal: Flow<Int>,
    private val hasUsageAccess: suspend () -> Boolean,
    private val syncUsage: suspend () -> Unit,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val state = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = state
    private val period = MutableStateFlow(InsightsPeriod.WEEK)
    private val today = MutableStateFlow(localDate())
    private val refreshVersion = MutableStateFlow(0)
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch(dispatcher) {
            try {
                val snapshots = combine(today, refreshVersion) { date, _ -> date }.flatMapLatest { date ->
                    flow {
                        emitAll(
                            store.observe(
                                LocalDay.startOfDay(date.minusDays(29), zone),
                                LocalDay.startOfDay(date, zone)
                            )
                        )
                    }.catch { failure ->
                        if (failure is CancellationException) throw failure
                        state.update { it.copy(loading = false, error = true) }
                    }
                }
                combine(snapshots, dailyGoalMs, pickupGoal, period) { snapshot, goal, pickups, range ->
                    val minutes = (goal / 60_000L).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
                    Triple(
                        InsightsSummary.build(snapshot, range, today.value, minutes, pickups, zone) to
                            Reflections.build(snapshot, today.value, minutes, pickups, zone),
                        minutes,
                        range
                    )
                }.collect { (data, goal, range) ->
                    state.update {
                        it.copy(
                            loading = false,
                            error = false,
                            summary = data.first,
                            reflection = data.second,
                            goalMinutes = goal,
                            period = range
                        )
                    }
                }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                state.update { it.copy(loading = false, error = true) }
            }
        }
        refresh()
    }

    fun setPeriod(value: InsightsPeriod) {
        period.value = value
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        today.value = localDate()
        refreshVersion.value++
        state.update { it.copy(refreshing = true, refreshError = false) }
        refreshJob = viewModelScope.launch(dispatcher) {
            try {
                val granted = hasUsageAccess()
                state.update { it.copy(hasUsageAccess = granted) }
                if (granted) syncUsage()
                val date = today.value
                val hours = if (granted) {
                    store.recentHours(
                        LocalDay.startOfDay(date.minusDays(6), zone),
                        LocalDay.startOfDay(date, zone)
                    )
                } else {
                    emptyList()
                }
                state.update { it.copy(hours = hours, refreshError = false) }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                state.update { it.copy(refreshError = true) }
            } finally {
                state.update { it.copy(refreshing = false) }
            }
        }
    }

    private fun localDate(): LocalDate = Instant.ofEpochMilli(nowMillis()).atZone(zone).toLocalDate()
}
