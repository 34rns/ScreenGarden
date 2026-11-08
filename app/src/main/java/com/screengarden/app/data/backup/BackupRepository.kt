// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.backup

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.withTransaction
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.data.local.ScreenGardenDatabase
import com.screengarden.app.data.local.entity.PendingRestore
import com.screengarden.app.domain.backup.BackupPreview
import com.screengarden.app.domain.backup.BackupStore
import com.screengarden.app.domain.backup.RestoreMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Bound SQL values, validated documents and a durable receipt protect a user's existing garden. */
class BackupRepository(
    private val database: ScreenGardenDatabase,
    settings: DataStore<Preferences>,
    private val now: () -> Long = System::currentTimeMillis
) : BackupStore {
    private val preferences = BackupPreferences(settings)
    private val mutex = Mutex()

    override fun preview(bytes: ByteArray): BackupPreview = BackupCodec.decode(bytes).preview()

    override suspend fun export(): ByteArray = withContext(Dispatchers.IO) {
        mutex.withLock {
            recoverLocked()
            val settings = preferences.snapshot()
            var totalRows = 0
            var estimatedBytes = settings.toString().toByteArray(Charsets.UTF_8).size + 1024
            val tables = database.withTransaction {
                BackupSchema.tables.mapValues { (name, table) ->
                    database.openHelper.writableDatabase.query(
                        "SELECT * FROM $name ORDER BY ${table.keys.joinToString()}"
                    )
                        .use { cursor ->
                            totalRows += cursor.count
                            require(totalRows <= BackupCodec.MAX_ROWS)
                            buildList {
                                while (cursor.moveToNext()) {
                                    val row = JSONObject().apply {
                                        table.columns.forEach { column ->
                                            val index = cursor.getColumnIndexOrThrow(column.name)
                                            put(
                                                column.name,
                                                when {
                                                    cursor.isNull(index) -> JSONObject.NULL
                                                    column.kind == "boolean" -> cursor.getInt(index) != 0
                                                    column.kind == "integer" -> cursor.getLong(index)
                                                    else -> cursor.getString(index)
                                                }
                                            )
                                        }
                                    }
                                    estimatedBytes += row.toString().toByteArray(Charsets.UTF_8).size + 1
                                    require(estimatedBytes <= BackupCodec.MAX_BYTES)
                                    add(row)
                                }
                            }
                        }
                }
            }
            BackupDocument(now(), settings, tables).encode().also { BackupCodec.decode(it) }
        }
    }

    override suspend fun restore(bytes: ByteArray, mode: RestoreMode): Unit = withContext(Dispatchers.IO) {
        // All content is validated before waiting for the write lock or changing a row.
        val document = BackupCodec.decode(bytes)
        mutex.withLock {
            recoverLocked()
            database.withTransaction {
                check(database.focusSessionDao().activeSession() == null) { "Stop focus before restoring" }
                val sql = database.openHelper.writableDatabase
                val today = LocalDay.startOfDay(now())
                if (mode == RestoreMode.REPLACE) {
                    BackupSchema.tables.keys.forEach { sql.execSQL("DELETE FROM $it") }
                }
                document.tables.forEach { (name, rows) ->
                    rows.forEach { source ->
                        val row = JSONObject(source.toString())
                        // Merge retains local plants, so old imported days must not reward them again.
                        // Replacement restores the matching plants and their original progress markers.
                        if (name == "daily_stats" &&
                            mode == RestoreMode.MERGE &&
                            row.getLong("dayStartUtcMillis") < today
                        ) {
                            row.put("gardenApplied", true)
                        }
                        if (name == "focus_sessions") {
                            row.put("bootCount", -1)
                            if (row.getString("status") == "RUNNING") {
                                row.put("status", "CANCELLED").put("wasCompleted", false)
                                    .put(
                                        "endedAtUtcMillis",
                                        maxOf(
                                            row.getLong("startedAtUtcMillis"),
                                            row.getLong("updatedAtUtcMillis")
                                        )
                                    )
                            }
                        }
                        val values = ContentValues()
                        BackupSchema.tables.getValue(name).columns.forEach { column ->
                            when (val value = row.get(column.name)) {
                                JSONObject.NULL -> values.putNull(column.name)
                                is Boolean -> values.put(column.name, if (value) 1 else 0)
                                is Number -> values.put(column.name, value.toLong())
                                is String -> values.put(column.name, value)
                            }
                        }
                        sql.insert(name, SQLiteDatabase.CONFLICT_IGNORE, values)
                    }
                }
                if (mode == RestoreMode.REPLACE) {
                    database.pendingRestoreDao().upsert(PendingRestore(settingsJson = document.settings.toString()))
                }
            }
            recoverLocked()
        }
    }

    /** The receipt survives process death. Clear only after DataStore confirms its atomic edit. */
    suspend fun recoverPendingSettings(): Unit = withContext(Dispatchers.IO) {
        mutex.withLock { recoverLocked() }
    }

    private suspend fun recoverLocked() {
        val receipt = database.pendingRestoreDao().get() ?: return
        preferences.apply(JSONObject(receipt.settingsJson))
        database.pendingRestoreDao().clear()
    }
}
