// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.settings

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.model.AccentColor
import com.screengarden.app.domain.model.ThemePreference
import com.screengarden.app.ui.onboarding.GoalSetup
import com.screengarden.app.ui.screens.help.BatteryHelpScreen

/** Scrollable, labelled preferences; permissions and writes stay with the caller. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onAppLimits: () -> Unit,
    modifier: Modifier = Modifier,
    onMilestones: () -> Unit = { },
    state: SettingsUiState = SettingsUiState(loading = false),
    actions: SettingsActions = SettingsActions(),
    notificationsAllowed: Boolean = true,
    onNotificationSettings: () -> Unit = { },
    versionName: String = "",
    environmentContent: (@Composable () -> Unit)? = null,
    backupContent: (@Composable () -> Unit)? = null,
    manufacturer: String = Build.MANUFACTURER,
    onAppSettings: () -> Unit = { }
) {
    var helpOpen by rememberSaveable { mutableStateOf(false) }
    if (helpOpen) {
        BackHandler { helpOpen = false }
        BatteryHelpScreen(manufacturer, { helpOpen = false }, onAppSettings, modifier)
        return
    }
    var goalsOpen by rememberSaveable { mutableStateOf(false) }
    var confirmIntro by rememberSaveable { mutableStateOf(false) }
    var choices by remember(state.goals) { mutableStateOf(state.goals) }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsHeading(R.string.tab_settings, main = true)
            if (state.loading || state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.error) SettingsBody(R.string.environment_save_error)
            if (state.saved) SettingsBody(R.string.settings_saved)
            SettingsHeading(R.string.settings_goals)
            OutlinedButton({ goalsOpen = !goalsOpen }, Modifier.fillMaxWidth(), enabled = !state.loading) {
                Text(stringResource(if (goalsOpen) R.string.settings_close_goals else R.string.settings_edit_goals))
            }
            if (goalsOpen) {
                GoalSetup(choices, { choices = it })
                Button(
                    { actions.saveGoals(choices) },
                    Modifier.fillMaxWidth(),
                    enabled =
                    !state.saving && !state.loading
                ) {
                    Text(stringResource(R.string.settings_save_goals))
                }
            }
            TextButton(onAppLimits, Modifier.fillMaxWidth()) { Text(stringResource(R.string.limits_title)) }
            TextButton(onMilestones, Modifier.fillMaxWidth()) { Text(stringResource(R.string.habits_title)) }
            HorizontalDivider()
            SettingsHeading(R.string.settings_notifications)
            SettingsToggle(
                R.string.settings_reminders,
                state.notificationsOn,
                actions.setNotifications,
                !state.saving && !state.loading
            )
            SettingsBody(R.string.settings_reminders_copy)
            if (!notificationsAllowed) SettingsBody(R.string.settings_notifications_off)
            TextButton(onNotificationSettings) { Text(stringResource(R.string.settings_notification_controls)) }
            HorizontalDivider()
            SettingsHeading(R.string.settings_appearance)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePreference.entries.forEach { choice ->
                    FilterChip(
                        state.theme == choice,
                        { actions.setTheme(choice) },
                        { Text(stringResource(themeLabel(choice))) },
                        Modifier.heightIn(min = 48.dp),
                        enabled = !state.loading && !state.saving
                    )
                }
            }
            SettingsBody(R.string.settings_accent)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentColor.entries.forEach { choice ->
                    FilterChip(
                        state.accent == choice,
                        { actions.setAccent(choice) },
                        { Text(stringResource(accentLabel(choice))) },
                        Modifier.heightIn(min = 48.dp),
                        enabled = !state.loading && !state.saving
                    )
                }
            }
            if (environmentContent != null) {
                environmentContent()
            } else {
                EnvironmentControls(
                    state.hemisphere,
                    state.lowPowerVisuals,
                    actions.setHemisphere,
                    actions.setLowPower,
                    saving = state.loading || state.saving
                )
            }
            HorizontalDivider()
            SettingsHeading(R.string.tab_focus)
            SettingsToggle(
                R.string.settings_keep_screen_on,
                state.keepScreenOn,
                actions.setKeepScreenOn,
                !state.loading && !state.saving
            )
            SettingsBody(R.string.settings_focus_copy)
            HorizontalDivider()
            SettingsHeading(R.string.settings_data)
            SettingsBody(R.string.settings_data_copy)
            backupContent?.invoke()
            TextButton({ confirmIntro = true }, enabled = !state.saving && !state.loading) {
                Text(stringResource(R.string.settings_show_intro))
            }
            HorizontalDivider()
            SettingsHeading(R.string.settings_about)
            TextButton({ helpOpen = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.battery_title)) }
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
            if (versionName.isNotBlank()) Text(stringResource(R.string.settings_version, versionName))
            SettingsBody(R.string.settings_privacy)
            SettingsBody(R.string.settings_license)
        }
    }
    if (confirmIntro) {
        AlertDialog(
            onDismissRequest = { confirmIntro = false },
            title = { Text(stringResource(R.string.settings_show_intro)) },
            text = { SettingsBody(R.string.settings_intro_copy) },
            confirmButton = {
                TextButton({
                    confirmIntro = false
                    actions.showIntroduction()
                }) { Text(stringResource(R.string.settings_show_intro)) }
            },
            dismissButton = { TextButton({ confirmIntro = false }) { Text(stringResource(R.string.settings_cancel)) } }
        )
    }
}

@Composable
private fun SettingsHeading(label: Int, main: Boolean = false) {
    Text(
        stringResource(label),
        Modifier.semantics { heading() },
        style = (if (main) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge)
            .copy(textDirection = TextDirection.Content)
    )
}

@Composable
private fun SettingsBody(label: Int) {
    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content))
}

@Composable
private fun SettingsToggle(label: Int, checked: Boolean, onChecked: (Boolean) -> Unit, enabled: Boolean) {
    val text = stringResource(label)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text, Modifier.weight(1f))
        Switch(checked, onChecked, Modifier.semantics { contentDescription = text }, enabled = enabled)
    }
}

private fun themeLabel(theme: ThemePreference): Int = when (theme) {
    ThemePreference.SYSTEM -> R.string.settings_system
    ThemePreference.LIGHT -> R.string.settings_light
    ThemePreference.DARK -> R.string.settings_dark
    ThemePreference.AMOLED -> R.string.settings_amoled
}

private fun accentLabel(accent: AccentColor): Int = when (accent) {
    AccentColor.CALM_GREEN -> R.string.settings_green
    AccentColor.SOFT_BLUE -> R.string.settings_blue
    AccentColor.HEATHER -> R.string.settings_heather
}
