// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screengarden.app.domain.garden.Hemisphere
import com.screengarden.app.domain.model.AccentColor
import com.screengarden.app.domain.model.OnboardingChoices
import com.screengarden.app.domain.model.ThemePreference
import com.screengarden.app.domain.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsActions(
    val saveGoals: (OnboardingChoices) -> Unit = { },
    val setTheme: (ThemePreference) -> Unit = { },
    val setAccent: (AccentColor) -> Unit = { },
    val setNotifications: (Boolean) -> Unit = { },
    val setKeepScreenOn: (Boolean) -> Unit = { },
    val setHemisphere: (Hemisphere) -> Unit = { },
    val setLowPower: (Boolean) -> Unit = { },
    val showIntroduction: () -> Unit = { }
)

data class SettingsUiState(
    val loading: Boolean = true,
    val goals: OnboardingChoices = OnboardingChoices(),
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val accent: AccentColor = AccentColor.CALM_GREEN,
    val notificationsOn: Boolean = false,
    val keepScreenOn: Boolean = false,
    val hemisphere: Hemisphere = Hemisphere.NORTHERN,
    val lowPowerVisuals: Boolean = false,
    val saving: Boolean = false,
    val error: Boolean = false,
    val saved: Boolean = false
)

/** One state for stored preferences; toggles never pretend a failed write succeeded. */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val state = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = state
    private var observer: Job? = null
    private var writeJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        observer?.cancel()
        observer = viewModelScope.launch(dispatcher) {
            try {
                val goals =
                    combine(
                        repository.dailyGoalMs,
                        repository.pickupGoal,
                        repository.bedtimeStart,
                        repository.bedtimeEnd
                    ) {
                            daily,
                            pickups,
                            start,
                            end
                        ->
                        OnboardingChoices.fromStored(daily, pickups, start, end)
                    }
                val appearance =
                    combine(
                        repository.theme,
                        repository.accentColor,
                        repository.hemisphere,
                        repository.lowPowerVisuals
                    ) {
                            theme,
                            accent,
                            side,
                            lowPower
                        ->
                        Appearance(theme, accent, side, lowPower)
                    }
                val behavior =
                    combine(repository.notificationsOn, repository.keepScreenOn) { reminders, keepOn ->
                        reminders to
                            keepOn
                    }
                combine(goals, appearance, behavior) { choices, look, behavior -> Triple(choices, look, behavior) }
                    .collect { (choices, look, behavior) ->
                        state.update {
                            it.copy(
                                loading = false,
                                goals = choices,
                                theme = look.theme,
                                accent = look.accent,
                                hemisphere = look.side,
                                lowPowerVisuals = look.lowPower,
                                notificationsOn = behavior.first,
                                keepScreenOn = behavior.second
                            )
                        }
                    }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                state.update { it.copy(loading = false, error = true) }
            }
        }
    }

    fun saveGoals(value: OnboardingChoices) = save { repository.setGoals(value) }
    fun setTheme(value: ThemePreference) = save { repository.setTheme(value) }
    fun setAccent(value: AccentColor) = save { repository.setAccentColor(value) }
    fun setNotifications(value: Boolean) = save { repository.setNotificationsOn(value) }
    fun setKeepScreenOn(value: Boolean) = save { repository.setKeepScreenOn(value) }
    fun setHemisphere(value: Hemisphere) = save { repository.setHemisphere(value) }
    fun setLowPower(value: Boolean) = save { repository.setLowPowerVisuals(value) }
    fun showIntroduction() = save { repository.setOnboardingDone(false) }

    private fun save(write: suspend () -> Unit) {
        if (writeJob?.isActive == true) return
        state.update { it.copy(saving = true, error = false, saved = false) }
        writeJob = viewModelScope.launch(dispatcher) {
            try {
                write()
                state.update { it.copy(saved = true) }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                state.update { it.copy(error = true) }
            } finally {
                state.update { it.copy(saving = false) }
            }
        }
    }

    private data class Appearance(
        val theme: ThemePreference,
        val accent: AccentColor,
        val side: Hemisphere,
        val lowPower: Boolean
    )
}
