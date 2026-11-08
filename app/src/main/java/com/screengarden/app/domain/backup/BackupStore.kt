// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.backup

enum class RestoreMode { MERGE, REPLACE }

data class BackupPreview(val createdAtUtcMillis: Long, val counts: Map<String, Int>)

/** Local documents only; validation finishes before any stored data changes. */
interface BackupStore {
    suspend fun export(): ByteArray
    fun preview(bytes: ByteArray): BackupPreview
    suspend fun restore(bytes: ByteArray, mode: RestoreMode)
}
