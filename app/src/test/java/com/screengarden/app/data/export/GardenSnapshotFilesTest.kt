// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GardenSnapshotFilesTest {
    @Test
    fun pendingImageSurvivesRecreationAndIsDiscardedAfterSaving() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val files = GardenSnapshotFiles(context)
        val first = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xff123456.toInt()) }
        val latest = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xff654321.toInt()) }
        files.prepare(first)
        files.prepare(latest)
        val destination = File(context.cacheDir, "fake-saved-snapshot.png")
        GardenSnapshotFiles(context).save(Uri.fromFile(destination))
        val decoded = BitmapFactory.decodeFile(destination.path)
        assertNotNull(decoded)
        assertEquals(0xff654321.toInt(), decoded.getPixel(16, 12))
        assertThrows(IOException::class.java) { files.save(Uri.fromFile(destination)) }
        destination.delete()
    }

    @Test
    fun cancelOrFailedDestinationDiscardsThePendingImage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val files = GardenSnapshotFiles(context)
        val bitmap = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888)
        files.prepare(bitmap)
        assertThrows(Exception::class.java) { files.save(Uri.parse("content://missing.fake.provider/snapshot")) }
        val destination = Uri.fromFile(File(context.cacheDir, "fake-unused.png"))
        assertThrows(IOException::class.java) { files.save(destination) }
        files.prepare(bitmap)
        files.discard()
        assertThrows(IOException::class.java) { files.save(destination) }
    }

    @Test fun writesAValidPngWithTheOriginalPixels() {
        val bitmap = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xff3f6b4e.toInt()) }
        val output = ByteArrayOutputStream()
        writeSnapshotPng(bitmap, output)
        val bytes = output.toByteArray()
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull("Save must produce an actual PNG", decoded)
        assertEquals(0xff3f6b4e.toInt(), decoded.getPixel(16, 12))
    }

    @Test fun aFailedWriteIsNeverReportedAsSuccess() {
        val bitmap = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888)
        val output = object : OutputStream() {
            override fun write(value: Int): Unit = throw IOException("Fake full storage")
            override fun write(bytes: ByteArray, offset: Int, length: Int): Unit =
                throw IOException("Fake full storage")
        }
        assertThrows(IOException::class.java) { writeSnapshotPng(bitmap, output) }
    }
}
