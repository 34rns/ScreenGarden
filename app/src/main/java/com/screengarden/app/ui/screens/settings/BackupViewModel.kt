// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screengarden.app.domain.backup.BackupPreview
import com.screengarden.app.domain.backup.BackupStore
import com.screengarden.app.domain.backup.RestoreMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BackupMessage { NONE, SAVED, RESTORED, READ_ERROR, WRITE_ERROR, RESTORE_ERROR }

data class BackupUiState(
    val busy: Boolean = false,
    val preview: BackupPreview? = null,
    val message: BackupMessage = BackupMessage.NONE
)

/** Keeps private document bytes across activity recreation, never in saved-state bundles or logs. */
class BackupViewModel(private val store: BackupStore) : ViewModel() {
    private val mutable = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = mutable
    private var pendingImport: ByteArray? = null
    private var pendingExport: ByteArray? = null

    fun prepareExport(openPicker: () -> Unit) = action(BackupMessage.WRITE_ERROR) {
        pendingExport = store.export()
        openPicker()
    }

    fun save(write: suspend (ByteArray) -> Unit) = action(BackupMessage.WRITE_ERROR) {
        val bytes = checkNotNull(pendingExport)
        write(bytes)
        pendingExport = null
        mutable.value = BackupUiState(message = BackupMessage.SAVED)
    }

    fun load(read: suspend () -> ByteArray) = action(BackupMessage.READ_ERROR) {
        pendingImport = null
        val bytes = read()
        val preview = withContext(Dispatchers.Default) { store.preview(bytes) }
        pendingImport = bytes
        mutable.value = BackupUiState(preview = preview)
    }

    fun restore(mode: RestoreMode) = action(BackupMessage.RESTORE_ERROR) {
        store.restore(checkNotNull(pendingImport), mode)
        pendingImport = null
        mutable.value = BackupUiState(message = BackupMessage.RESTORED)
    }

    fun cancelPreview() {
        if (!mutable.value.busy) {
            pendingImport = null
            mutable.value = BackupUiState()
        }
    }

    fun cancelExport() {
        pendingExport = null
    }

    private fun action(error: BackupMessage, run: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy = true, message = BackupMessage.NONE)
        viewModelScope.launch {
            try {
                run()
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                mutable.value = mutable.value.copy(message = error)
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }
}
