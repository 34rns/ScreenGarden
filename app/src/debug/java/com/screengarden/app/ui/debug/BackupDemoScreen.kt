// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.backup.BackupPreview
import com.screengarden.app.ui.screens.settings.BackupControls
import com.screengarden.app.ui.screens.settings.BackupMessage
import com.screengarden.app.ui.screens.settings.BackupUiState
import java.time.LocalDate

/** Fake presentation only: no document URI, database, DataStore or usage read. */
@Composable
fun BackupDemoScreen(openPreview: Boolean, modifier: Modifier = Modifier) {
    val preview = BackupPreview(
        LocalDay.startOfDay(LocalDate.of(2026, 11, 1)),
        linkedMapOf(
            "daily_stats" to 7,
            "app_usage_daily" to 21,
            "plants" to 5,
            "focus_sessions" to 12,
            "badges" to 3,
            "app_limits" to 2,
            "garden_history" to 4
        )
    )
    var state by remember { mutableStateOf(BackupUiState(preview = if (openPreview) preview else null)) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(stringResource(R.string.settings_data), style = MaterialTheme.typography.headlineLarge)
        BackupControls(
            state,
            { state = BackupUiState(message = BackupMessage.SAVED) },
            { state = BackupUiState(preview = preview) },
            { state = BackupUiState(message = BackupMessage.RESTORED) },
            { state = BackupUiState() }
        )
    }
}
