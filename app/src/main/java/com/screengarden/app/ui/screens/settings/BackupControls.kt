// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.backup.RestoreMode
import java.text.DateFormat
import java.util.Date

/** Merge is the first choice; replacing requires a separate explicit confirmation. */
@Composable
fun BackupControls(
    state: BackupUiState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onRestore: (RestoreMode) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var replaceConfirm by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BackupText(R.string.backup_privacy)
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(onExport, Modifier.fillMaxWidth(), enabled = !state.busy && state.preview == null) {
            Text(stringResource(R.string.backup_export))
        }
        OutlinedButton(onImport, Modifier.fillMaxWidth(), enabled = !state.busy && state.preview == null) {
            Text(stringResource(R.string.backup_import))
        }
        if (state.message != BackupMessage.NONE) {
            Text(
                stringResource(messageLabel(state.message)),
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
            )
        }
    }
    val preview = state.preview
    if (preview != null && !replaceConfirm) {
        AlertDialog(
            onDismissRequest = { if (!state.busy) onCancel() },
            title = { Text(stringResource(R.string.backup_preview)) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(
                            R.string.backup_created,
                            DateFormat.getDateTimeInstance().format(Date(preview.createdAtUtcMillis))
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
                    )
                    preview.counts.forEach { (key, count) ->
                        Text(
                            stringResource(R.string.backup_count, stringResource(tableLabel(key)), count),
                            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
                        )
                    }
                    BackupText(R.string.backup_merge_copy)
                    BackupText(R.string.backup_replace_copy)
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (state.message == BackupMessage.RESTORE_ERROR) BackupText(R.string.backup_restore_error)
                }
            },
            confirmButton = {
                Column {
                    TextButton({ onRestore(RestoreMode.MERGE) }, enabled = !state.busy) {
                        Text(stringResource(R.string.backup_merge))
                    }
                    TextButton({ replaceConfirm = true }, enabled = !state.busy) {
                        Text(stringResource(R.string.backup_replace))
                    }
                }
            },
            dismissButton = {
                TextButton(onCancel, enabled = !state.busy) { Text(stringResource(R.string.settings_cancel)) }
            }
        )
    }
    if (replaceConfirm && preview != null) {
        AlertDialog(
            onDismissRequest = { if (!state.busy) replaceConfirm = false },
            title = { Text(stringResource(R.string.backup_replace_question)) },
            text = { BackupText(R.string.backup_replace_warning) },
            confirmButton = {
                TextButton({
                    replaceConfirm = false
                    onRestore(RestoreMode.REPLACE)
                }, enabled = !state.busy) {
                    Text(stringResource(R.string.backup_replace))
                }
            },
            dismissButton = {
                TextButton({ replaceConfirm = false }, enabled = !state.busy) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }
}

@Composable
private fun BackupText(label: Int) {
    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content))
}

private fun messageLabel(message: BackupMessage): Int = when (message) {
    BackupMessage.SAVED -> R.string.backup_saved
    BackupMessage.RESTORED -> R.string.backup_restored
    BackupMessage.READ_ERROR -> R.string.backup_read_error
    BackupMessage.WRITE_ERROR -> R.string.backup_write_error
    BackupMessage.RESTORE_ERROR -> R.string.backup_restore_error
    BackupMessage.NONE -> R.string.backup_privacy
}

private fun tableLabel(table: String): Int = when (table) {
    "daily_stats" -> R.string.backup_days
    "app_usage_daily" -> R.string.backup_usage
    "plants" -> R.string.backup_plants
    "focus_sessions" -> R.string.backup_sessions
    "badges" -> R.string.backup_badges
    "app_limits" -> R.string.backup_limits
    else -> R.string.backup_history
}
