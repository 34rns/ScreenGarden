// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screengarden.app.core.time.BedtimeWindow
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.garden.GardenDay
import com.screengarden.app.domain.garden.GardenDayRecord
import com.screengarden.app.domain.garden.GardenSnapshot
import com.screengarden.app.domain.garden.GardenStore
import com.screengarden.app.domain.garden.HealthScoreCalculator
import com.screengarden.app.domain.garden.HealthScoreInputs
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One garden state; reads and once-only completed-day writes run off the main thread. */
@OptIn(ExperimentalCoroutinesApi::class)
class GardenViewModel(
    private val store: GardenStore,
    dailyGoalMs: Flow<Long>,
    pickupGoal: Flow<Int>,
    streakThreshold: Flow<Int>,
    private val hasUsageAccess: suspend () -> Boolean,
    private val syncUsage: suspend () -> Unit,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    bedtimeWindow: Flow<BedtimeWindow?> = flowOf(null)
) : ViewModel() {

    private val mutableState = MutableStateFlow(GardenUiState())
    val uiState: StateFlow<GardenUiState> = mutableState
    private val today = MutableStateFlow(LocalDay.startOfDay(nowMillis(), zoneId))
    private val permission = MutableStateFlow(false)
    private val refreshVersion = MutableStateFlow(0)
    private val moment = MutableStateFlow(nowMillis())
    private var refreshJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch(dispatcher) {
            try {
                val snapshots = combine(today, refreshVersion) { day, _ -> day }.flatMapLatest { day ->
                    var from = day
                    repeat(31) { from = LocalDay.previousDayStart(from, zoneId) }
                    store.observeSnapshot(from, day).catch { failure ->
                        if (failure is CancellationException) throw failure
                        mutableState.update { it.copy(loading = false, error = true) }
                    }
                }
                val goals = combine(dailyGoalMs, pickupGoal, streakThreshold) { daily, pickups, threshold ->
                    Goals((daily / 60_000L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(), pickups, threshold)
                }
                launch {
                    combine(bedtimeWindow, moment) { window, now ->
                        window?.contains(Instant.ofEpochMilli(now).atZone(zoneId).toLocalTime()) == true
                    }.collect { night -> mutableState.update { it.copy(isBedtime = night) } }
                }
                combine(snapshots, goals, permission) { snapshot, settings, granted ->
                    Triple(snapshot, settings, granted)
                }.collect { (snapshot, settings, granted) ->
                    show(snapshot, settings, granted)
                    if (granted) advanceCompletedDays(snapshot, settings)
                }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                mutableState.update { it.copy(loading = false, error = true) }
            }
        }
        refresh()
    }

    /** Called when returning from system settings, or requesting another local sync. */
    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch(dispatcher) {
            try {
                updateTime()
                permission.value = hasUsageAccess()
                if (permission.value) syncUsage()
                mutableState.update { it.copy(loading = false, error = false) }
                refreshVersion.update { it + 1 }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                mutableState.update { it.copy(loading = false, error = true) }
            }
        }
    }

    /** Visible-screen minute tick changes presentation only; it does not repeatedly query usage. */
    fun updateTime() {
        moment.value = nowMillis()
        today.value = LocalDay.startOfDay(moment.value, zoneId)
    }

    private fun show(snapshot: GardenSnapshot, goals: Goals, granted: Boolean) {
        val current = snapshot.days.firstOrNull { it.dayStartUtcMillis == today.value }
        val availability = when {
            !granted -> GardenAvailability.NO_PERMISSION
            current == null -> GardenAvailability.NO_DATA
            !current.usageIsBounded -> GardenAvailability.INCOMPLETE_USAGE
            else -> GardenAvailability.READY
        }
        mutableState.update { previous ->
            GardenUiState(
                loading = false,
                availability = availability,
                plants = snapshot.plants,
                score = if (granted && current?.usageIsBounded == true) score(current, goals) else null,
                streakDays = streak(snapshot.days, LocalDay.previousDayStart(today.value, zoneId), goals.threshold),
                error = previous.error,
                isBedtime = previous.isBedtime
            )
        }
    }

    private suspend fun advanceCompletedDays(snapshot: GardenSnapshot, goals: Goals) {
        val scoredDays = snapshot.days.map { day ->
            if (day.usageIsBounded && !day.gardenApplied) day.copy(score = score(day, goals)) else day
        }
        for (day in snapshot.days.sortedBy { it.dayStartUtcMillis }) {
            if (day.dayStartUtcMillis >= today.value || day.gardenApplied || !day.usageIsBounded) continue
            val calculated = score(day, goals)
            if (day.score != calculated &&
                !store.recordScore(day.dayStartUtcMillis, calculated, day.updatedAtUtcMillis)
            ) {
                continue
            }
            store.advanceCompletedDay(
                GardenDay(
                    score = calculated,
                    focusSessionsCompleted = day.sessionsCompleted,
                    streakDays = streak(scoredDays, day.dayStartUtcMillis, goals.threshold),
                    dayStartUtcMillis = day.dayStartUtcMillis,
                    focusRewardsAlreadyApplied = true
                )
            )
        }
    }

    private fun score(day: GardenDayRecord, goals: Goals): Int? = HealthScoreCalculator.calculate(
        HealthScoreInputs(
            screenMinutes = day.screenMinutes,
            dailyGoalMinutes = goals.dailyMinutes,
            pickups = day.pickups,
            pickupGoal = goals.pickups,
            focusMinutes = day.focusMinutes,
            bedtimeMinutes = day.bedtimeMinutes,
            limitOveragesMinutes = day.limitOveragesMinutes
        )
    )?.value

    private fun streak(days: List<GardenDayRecord>, end: Long, threshold: Int): Int {
        val byDay = days.associateBy { it.dayStartUtcMillis }
        var dayKey = end
        var count = 0
        while (true) {
            val day = byDay[dayKey] ?: break
            if (!day.usageIsBounded || (day.score ?: break) < threshold) break
            count++
            dayKey = LocalDay.previousDayStart(dayKey, zoneId)
        }
        return count
    }

    private data class Goals(val dailyMinutes: Int, val pickups: Int, val threshold: Int)
}
