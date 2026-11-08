// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.backup

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupCodecTest {
    private fun empty(): JSONObject = JSONObject()
        .put("format", "ScreenGarden").put("schemaVersion", 1).put("createdAtUtcMillis", 1000L)
        .put("settings", BackupPreferences.defaults())
        .put("tables", JSONObject().apply { BackupSchema.tables.keys.forEach { put(it, JSONArray()) } })

    @Test
    fun emptyBackupRoundTripsEveryTableAndSetting() {
        val parsed = BackupCodec.decode(empty().toString().toByteArray())
        assertEquals(7, parsed.tables.size)
        assertEquals(14, parsed.settings.length())
        assertEquals(1000L, parsed.createdAtUtcMillis)
    }

    @Test
    fun corruptFutureOlderAndTrailingFilesAreRejected() {
        for (text in listOf(
            "broken",
            "{}",
            empty().put("schemaVersion", 0).toString(),
            empty().put("schemaVersion", 2).toString(),
            empty().toString() + "{}"
        )) {
            assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(text.toByteArray()) }
        }
    }

    @Test
    fun wrongTypesUnknownTablesUnsafeValuesAndDuplicateKeysAreRejected() {
        val bad = empty()
        bad.getJSONObject("settings").put("notifications_on", "true")
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(bad.toString().toByteArray()) }
        val unknown = empty()
        unknown.getJSONObject("tables").put("sqlite_master", JSONArray())
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(unknown.toString().toByteArray()) }
        val rows = JSONArray().put(JSONObject().put("badgeKey", "first-bloom").put("unlockedAtUtcMillis", -1))
        val negative = empty()
        negative.getJSONObject("tables").put("badges", rows)
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(negative.toString().toByteArray()) }
        rows.getJSONObject(0).put("unlockedAtUtcMillis", 100)
        rows.put(JSONObject(rows.getJSONObject(0).toString()))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(negative.toString().toByteArray()) }
    }

    @Test
    fun oversizedFilesAreRejectedBeforeParsing() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(ByteArray(BackupCodec.MAX_BYTES + 1)) }
    }
}
