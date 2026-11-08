// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.export

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.AtomicFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream

/** One app-private pending PNG survives the external document picker and activity recreation. */
class GardenSnapshotFiles(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val directory = File(context.applicationContext.cacheDir, "garden-snapshot")
    private val pending = AtomicFile(File(directory, "pending.png"))

    fun prepare(bitmap: Bitmap) {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Snapshot cache unavailable")
        // A new save replaces any abandoned picker image; never leave a stale picture pending.
        pending.delete()
        val output = pending.startWrite()
        try {
            writeSnapshotPng(bitmap, output)
            pending.finishWrite(output)
        } catch (failure: Exception) {
            pending.failWrite(output)
            throw failure
        }
    }

    /** Writes only to the URI Android's CreateDocument picker returned. */
    fun save(destination: Uri) {
        try {
            pending.openRead().use { input ->
                val output = resolver.openOutputStream(destination, "wt") ?: throw IOException("Document unavailable")
                output.use { input.copyTo(it) }
            }
        } finally {
            discard()
        }
    }

    fun discard() {
        pending.delete()
    }
}

internal fun writeSnapshotPng(bitmap: Bitmap, output: OutputStream) {
    // Encode first so native Bitmap compression cannot swallow the destination's IOException.
    val encoded = ByteArrayOutputStream()
    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, encoded)) throw IOException("PNG encoding failed")
    encoded.writeTo(output)
}
