// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Skeleton for schema migration tests (D22).
 *
 * The schemas are exported to `app/schemas/` and added to the androidTest assets
 * in app/build.gradle.kts, so [MigrationTestHelper] can create a database at any
 * historical version and run the migrations under test.
 *
 * These run on a device/emulator (`./gradlew :app:connectedDebugAndroidTest`),
 * not on the JVM - the JVM suite covers the DAOs instead. The DAO tests prove the
 * current schema works; THIS file is what proves an old install survives an
 * upgrade. Never bump [ScreenGardenDatabase.VERSION] without adding a case here.
 */
@RunWith(AndroidJUnit4::class)
class ScreenGardenDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ScreenGardenDatabase::class.java
    )

    @Test
    fun currentSchemaCanBeCreatedAndValidated() {
        helper.createDatabase(TEST_DATABASE, ScreenGardenDatabase.VERSION).close()

        helper.runMigrationsAndValidate(TEST_DATABASE, ScreenGardenDatabase.VERSION, true).close()

        assertTrue(ScreenGardenDatabase.VERSION >= 1)
    }

    /**
     * Template for the first real migration. Enable it when version 2 exists:
     * 1. create the database at version 1,
     * 2. insert a representative row with raw SQL,
     * 3. run the migration and validate the new schema,
     * 4. assert the row survived with the expected values.
     */
    @Ignore("Template: enable and fill in when schema version 2 is added (D22)")
    @Test
    fun migratingFrom1To2KeepsExistingRows() {
        // val oldDatabase = helper.createDatabase(TEST_DATABASE, 1)
        // oldDatabase.execSQL(
        //     "INSERT INTO daily_stats " +
        //         "(dayStartUtcMillis, focusMinutes, screenMinutes, sessionsCompleted, " +
        //         "plantsGrown, updatedAtUtcMillis) " +
        //         "VALUES (1700000000000, 25, 90, 1, 1, 1700000000000)"
        // )
        // oldDatabase.close()
        //
        // val migrated = helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, MIGRATION_1_2)
        // migrated.query("SELECT focusMinutes FROM daily_stats").use { cursor ->
        //     assertTrue(cursor.moveToFirst())
        //     assertEquals(25, cursor.getInt(0))
        // }
        // migrated.close()
    }

    private companion object {
        const val TEST_DATABASE = "migration-test.db"
    }
}
