// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.screengarden.app.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The preview freezes the opened garden and sends exactly that rendered image to the caller. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GardenSnapshotPreview(
    state: GardenUiState,
    onSave: (ImageBitmap) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot = remember { state.copy(plants = state.plants.toList()) }
    val date = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
            .format(Instant.now().atZone(ZoneId.systemDefault()))
    }
    var includeCaption by rememberSaveable { mutableStateOf(true) }
    val caption = stringResource(
        R.string.snapshot_caption,
        stringResource(R.string.app_name),
        date,
        pluralStringResource(R.plurals.garden_streak_days, snapshot.streakDays, snapshot.streakDays)
    )
    val colors = MaterialTheme.colorScheme
    val renderer = LocalPlantRenderer.current
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var error by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(includeCaption, colors, renderer, retry) {
        image = null
        error = false
        try {
            image = withContext(Dispatchers.Default) {
                renderGardenSnapshot(snapshot, colors, if (includeCaption) caption else null, renderer)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = true
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        val dialogView = LocalView.current
        SideEffect {
            // Bottom sheets own a separate window; the Activity's theme flags do not reach it.
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, dialogView).apply {
                    isAppearanceLightStatusBars = colors.background.luminance() > 0.5f
                    isAppearanceLightNavigationBars = colors.background.luminance() > 0.5f
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.snapshot_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Content)
            )
            Text(
                stringResource(R.string.snapshot_description),
                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
            )
            val readyImage = image
            // Reserve the image area while rendering so the sheet's anchors do not jump on load.
            Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f), contentAlignment = Alignment.Center) {
                if (readyImage != null) {
                    Image(
                        readyImage,
                        contentDescription = stringResource(R.string.snapshot_preview_description),
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (error) {
                            Text(
                                stringResource(R.string.snapshot_render_error),
                                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
                            )
                            TextButton(onClick = { retry++ }) { Text(stringResource(R.string.snapshot_retry)) }
                        } else {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(stringResource(R.string.snapshot_rendering))
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().toggleable(includeCaption, role = Role.Checkbox) { includeCaption = it },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(includeCaption, onCheckedChange = null)
                Text(stringResource(R.string.snapshot_include_caption))
            }
            Button(
                onClick = { image?.let(onSave) },
                enabled = readyImage != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.snapshot_save)) }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.snapshot_close))
            }
        }
    }
}
