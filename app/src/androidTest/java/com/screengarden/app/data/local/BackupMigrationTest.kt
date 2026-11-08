// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ScreenGardenDatabase::class.java
    )

    @Test
    fun v6DataSurvivesAndReceiptStartsEmpty() {
        helper.createDatabase("backup-migration.db", 6).use { db ->
            db.execSQL("INSERT INTO badges VALUES ('first-bloom', 1700000000000)")
            db.execSQL("INSERT INTO garden_history VALUES (1700000000000, 1700000000001, 84, 'original-history')")
        }
        helper.runMigrationsAndValidate(
            "backup-migration.db",
            7,
            true,
            ScreenGardenDatabase.MIGRATION_6_7
        ).use { db ->
            db.query("SELECT * FROM badges").use { assertEquals(1, it.count) }
            db.query("SELECT plantsJson FROM garden_history").use {
                assertTrue(it.moveToFirst())
                assertEquals("original-history", it.getString(0))
            }
            db.query("SELECT * FROM pending_restore").use { assertEquals(0, it.count) }
        }
    }

    @Test
    fun originalSchemaReaches7WithoutDeletingRows() {
        helper.createDatabase("backup-chain.db", 1).use {
            it.execSQL("INSERT INTO badges VALUES ('first-bloom', 1700000000000)")
        }
        helper.runMigrationsAndValidate(
            "backup-chain.db", 7, true,
            ScreenGardenDatabase.MIGRATION_1_2, ScreenGardenDatabase.MIGRATION_2_3,
            ScreenGardenDatabase.MIGRATION_3_4, ScreenGardenDatabase.MIGRATION_4_5,
            ScreenGardenDatabase.MIGRATION_5_6, ScreenGardenDatabase.MIGRATION_6_7
        ).use { db ->
            db.query("SELECT * FROM badges").use { assertEquals(1, it.count) }
        }
    }
}
