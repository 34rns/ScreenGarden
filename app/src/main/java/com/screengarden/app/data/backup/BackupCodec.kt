// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.backup

import com.screengarden.app.data.repo.GardenHistoryCodec
import com.screengarden.app.domain.backup.BackupPreview
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

internal data class BackupDocument(
    val createdAtUtcMillis: Long,
    val settings: JSONObject,
    val tables: Map<String, List<JSONObject>>
) {
    fun preview(): BackupPreview = BackupPreview(createdAtUtcMillis, tables.mapValues { it.value.size })
    fun encode(): ByteArray = JSONObject().put("format", "ScreenGarden").put("schemaVersion", 1)
        .put("createdAtUtcMillis", createdAtUtcMillis).put("settings", settings)
        .put("tables", JSONObject().apply { tables.forEach { (name, rows) -> put(name, JSONArray(rows)) } })
        .toString().toByteArray(Charsets.UTF_8)
}

/** Schema v1 has no older published format. Unknown versions fail before a write. */
internal object BackupCodec {
    const val MAX_BYTES: Int = 32 * 1024 * 1024
    const val MAX_ROWS: Int = 100_000

    fun decode(bytes: ByteArray): BackupDocument = try {
        require(bytes.size in 1..MAX_BYTES)
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        val parser = JSONTokener(text)
        val root = parser.nextValue() as? JSONObject ?: error("Object required")
        require(parser.nextClean() == '\u0000')
        require(
            root.keys().asSequence().toSet() == setOf(
                "format",
                "schemaVersion",
                "createdAtUtcMillis",
                "settings",
                "tables"
            )
        )
        require(root.get("format") == "ScreenGarden")
        require(integer(root.get("schemaVersion"), 1, 1) == 1L)
        val created = integer(root.get("createdAtUtcMillis"), 0, Long.MAX_VALUE)
        val settings = root.getJSONObject("settings").also { BackupPreferences.validate(it) }
        val source = root.getJSONObject("tables")
        require(source.keys().asSequence().toSet() == BackupSchema.tables.keys)
        var count = 0
        val tables = BackupSchema.tables.mapValues { (name, table) ->
            val array = source.getJSONArray(name)
            count += array.length()
            require(count <= MAX_ROWS)
            val keys = mutableSetOf<List<Any>>()
            (0 until array.length()).map { index ->
                val row = array.getJSONObject(index)
                require(row.keys().asSequence().toSet() == table.columns.map { it.name }.toSet())
                table.columns.forEach { it.validate(row.get(it.name)) }
                require(keys.add(table.keys.map { row.get(it) }))
                if (name == "garden_history") GardenHistoryCodec.decode(row.getString("plantsJson"))
                if (name == "focus_sessions") {
                    val completed = row.getBoolean("wasCompleted")
                    require(completed == (row.getString("status") == "COMPLETED"))
                    require((row.getString("status") == "RUNNING") == row.isNull("endedAtUtcMillis"))
                    if (!row.isNull("endedAtUtcMillis")) {
                        require(row.getLong("endedAtUtcMillis") >= row.getLong("startedAtUtcMillis"))
                    }
                    require(row.getLong("actualDurationMs") <= row.getLong("plannedMinutes") * 60_000)
                }
                row
            }
        }
        BackupDocument(created, settings, tables)
    } catch (failure: Exception) {
        throw IllegalArgumentException("Invalid, oversized or unsupported ScreenGarden backup", failure)
    }

    fun integer(value: Any, min: Long, max: Long): Long {
        require(value is Int || value is Long)
        return (value as Number).toLong().also { require(it in min..max) }
    }
}

internal data class BackupColumn(
    val name: String,
    val kind: String,
    val nullable: Boolean = false,
    val min: Long = 0,
    val max: Long = Int.MAX_VALUE.toLong(),
    val options: Set<String> = emptySet()
) {
    fun validate(value: Any) {
        if (value == JSONObject.NULL) {
            require(nullable)
            return
        }
        when (kind) {
            "integer" -> BackupCodec.integer(value, min, max)
            "boolean" -> require(value is Boolean)
            "text" -> {
                require(value is String && value.isNotBlank() && value.length <= max)
                require(value.none { it.code < 32 })
                if (options.isNotEmpty()) require(value in options)
            }
            else -> error("Unsupported column")
        }
    }
}

internal data class BackupTable(val columns: List<BackupColumn>, val keys: List<String>)
