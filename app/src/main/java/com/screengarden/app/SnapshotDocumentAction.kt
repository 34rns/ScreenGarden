// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import com.screengarden.app.data.export.GardenSnapshotFiles
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Composition-root adapter; the preview never accesses files or launches external activities. */
@Composable
internal fun rememberSnapshotSaveAction(): (ImageBitmap) -> Unit {
    val context = LocalContext.current
    val files = remember(context.applicationContext) { GardenSnapshotFiles(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    fun report(id: Int) {
        Toast.makeText(context, id, Toast.LENGTH_LONG).show()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (uri == null) files.discard() else files.save(uri)
                }
                if (uri != null) report(R.string.snapshot_saved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                report(R.string.snapshot_save_error)
            } finally {
                busy = false
            }
        }
    }
    return { image ->
        if (!busy) {
            busy = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { files.prepare(image.asAndroidBitmap()) }
                    picker.launch("ScreenGarden.png")
                } catch (cancelled: CancellationException) {
                    busy = false
                    throw cancelled
                } catch (_: Exception) {
                    withContext(Dispatchers.IO) { files.discard() }
                    busy = false
                    report(R.string.snapshot_save_error)
                }
            }
        }
    }
}
