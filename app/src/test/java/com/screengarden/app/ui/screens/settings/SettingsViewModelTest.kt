// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.screengarden.app.data.settings.DataStoreSettingsRepository
import com.screengarden.app.domain.model.AccentColor
import com.screengarden.app.domain.model.OnboardingChoices
import com.screengarden.app.domain.model.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun savedPreferencesDriveOneStateAndCanBeChangedAgain() = runTest(dispatcher) {
        val repository = DataStoreSettingsRepository(Store())
        val model = SettingsViewModel(repository, dispatcher)
        advanceUntilIdle()
        assertFalse(model.uiState.value.loading)
        model.setTheme(ThemePreference.AMOLED)
        advanceUntilIdle()
        model.setAccent(AccentColor.SOFT_BLUE)
        advanceUntilIdle()
        model.saveGoals(OnboardingChoices(dailyGoalMs = 120 * 60_000L))
        advanceUntilIdle()
        assertEquals(ThemePreference.AMOLED, model.uiState.value.theme)
        assertEquals(AccentColor.SOFT_BLUE, model.uiState.value.accent)
        assertEquals(120 * 60_000L, model.uiState.value.goals.dailyGoalMs)
        assertTrue(model.uiState.value.saved)
        assertFalse(model.uiState.value.saving)
    }

    @Test fun failedWriteRetainsStoredChoiceAndNextAttemptCanRecover() = runTest(dispatcher) {
        val store = Store()
        val model = SettingsViewModel(DataStoreSettingsRepository(store), dispatcher)
        advanceUntilIdle()
        store.fail = true
        model.setTheme(ThemePreference.DARK)
        advanceUntilIdle()
        assertTrue(model.uiState.value.error)
        assertFalse(model.uiState.value.saving)
        assertEquals(ThemePreference.SYSTEM, model.uiState.value.theme)
        store.fail = false
        model.setTheme(ThemePreference.DARK)
        advanceUntilIdle()
        assertFalse(model.uiState.value.error)
        assertEquals(ThemePreference.DARK, model.uiState.value.theme)
    }

    @Test fun introductionResetsOnlyItsCompletionFlag() = runTest(dispatcher) {
        val repository = DataStoreSettingsRepository(Store())
        repository.setOnboardingDone(true)
        repository.setTheme(ThemePreference.DARK)
        val model = SettingsViewModel(repository, dispatcher)
        advanceUntilIdle()
        model.showIntroduction()
        advanceUntilIdle()
        assertFalse(repository.onboardingDone.first())
        assertEquals(ThemePreference.DARK, repository.theme.first())
    }

    private class Store : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        var fail = false
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            if (fail) error("fake write failure")
            return transform(data.value).also { data.value = it }
        }
    }
}
