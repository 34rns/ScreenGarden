// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.backup

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Read/write only the URI returned by Android's picker; no storage-wide access. */
class BackupDocuments(private val resolver: ContentResolver) {
    suspend fun read(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        (resolver.openInputStream(uri) ?: throw IOException("Document unavailable")).use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= BackupCodec.MAX_BYTES)
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }

    suspend fun write(uri: Uri, bytes: ByteArray): Unit = withContext(Dispatchers.IO) {
        require(bytes.size <= BackupCodec.MAX_BYTES)
        (resolver.openOutputStream(uri, "wt") ?: throw IOException("Document unavailable")).use { output ->
            output.write(bytes)
            output.flush()
        }
    }
}
