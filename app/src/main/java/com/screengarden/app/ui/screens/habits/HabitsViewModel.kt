// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.habits.CompassionResult
import com.screengarden.app.domain.habits.HabitRules
import com.screengarden.app.domain.habits.HabitsSnapshot
import com.screengarden.app.domain.habits.HabitsStore
import com.screengarden.app.domain.habits.Streaks
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HabitsUiState(
    val loading: Boolean = true,
    val snapshot: HabitsSnapshot = HabitsSnapshot(emptyList(), emptyList(), emptyList()),
    val streaks: Streaks = Streaks(0, 0),
    val threshold: Int = 60,
    val monthlyLimit: Int = 3,
    val today: LocalDate = LocalDate.now(),
    val error: Boolean = false,
    val saving: Boolean = false,
    val message: HabitsMessage? = null
)

enum class HabitsMessage { SAVED, INVALID_DATE, FUTURE_DATE, LIMIT_REACHED, SAVE_ERROR }

/** One observed state; write failures remain visible and do not optimistically alter stored history. */
class HabitsViewModel(
    private val store: HabitsStore,
    threshold: Flow<Int>,
    monthlyLimit: Flow<Int>,
    private val saveThreshold: suspend (Int) -> Unit,
    private val saveMonthlyLimit: suspend (Int) -> Unit,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val state =
        MutableStateFlow(HabitsUiState(today = Instant.ofEpochMilli(nowMillis()).atZone(zone).toLocalDate()))
    val uiState: StateFlow<HabitsUiState> = state

    init {
        viewModelScope.launch(dispatcher) {
            try {
                combine(store.observe(), threshold, monthlyLimit) { snapshot, goal, limit ->
                    Triple(snapshot, goal, limit)
                }
                    .collect { (snapshot, goal, limit) ->
                        val today = Instant.ofEpochMilli(nowMillis()).atZone(zone).toLocalDate()
                        val todayKey = LocalDay.startOfDay(today, zone)
                        val row = snapshot.days.firstOrNull { it.dayStartUtcMillis == todayKey }
                        val end = if (row != null && (row.isCompassionDay || HabitRules.qualifies(row, goal))) {
                            todayKey
                        } else {
                            LocalDay.previousDayStart(todayKey, zone)
                        }
                        state.update {
                            it.copy(
                                loading = false,
                                snapshot = snapshot,
                                threshold = goal,
                                monthlyLimit = limit,
                                today = today,
                                streaks = HabitRules.streaks(snapshot.days, end, goal, zone),
                                error = false
                            )
                        }
                        store.evaluateBadges(goal)
                    }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                state.update { it.copy(loading = false, error = true) }
            }
        }
    }

    fun setCompassionDay(text: String, enabled: Boolean) {
        val date = runCatching { LocalDate.parse(text) }.getOrNull()
        if (date == null) {
            state.update { it.copy(message = HabitsMessage.INVALID_DATE) }
            return
        }
        write {
            val result = store.setCompassionDay(date, enabled, state.value.monthlyLimit)
            state.update {
                it.copy(
                    message = when (result) {
                        CompassionResult.SAVED -> HabitsMessage.SAVED
                        CompassionResult.LIMIT_REACHED -> HabitsMessage.LIMIT_REACHED
                        CompassionResult.FUTURE_DATE -> HabitsMessage.FUTURE_DATE
                    }
                )
            }
        }
    }

    fun setThreshold(value: Int) = write { saveThreshold(value) }
    fun setMonthlyLimit(value: Int) = write { saveMonthlyLimit(value) }

    private fun write(action: suspend () -> Unit) {
        if (state.value.saving) return
        state.update { it.copy(saving = true, message = null) }
        viewModelScope.launch(dispatcher) {
            try {
                action()
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                state.update { it.copy(message = HabitsMessage.SAVE_ERROR) }
            } finally {
                state.update { it.copy(saving = false) }
            }
        }
    }
}
