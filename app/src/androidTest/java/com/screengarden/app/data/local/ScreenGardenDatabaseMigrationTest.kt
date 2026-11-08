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

/**
 * Schema migration preservation tests (D22).
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
    @Test
    fun migratingFrom5To6KeepsExistingDataAndDoesNotInventOldSnapshots() {
        helper.createDatabase(TEST_DATABASE, 5).use { old ->
            old.execSQL(
                "INSERT INTO daily_stats (dayStartUtcMillis, focusMinutes, screenMinutes, sessionsCompleted, " +
                    "plantsGrown, updatedAtUtcMillis, score, usageIsBounded, gardenApplied, isCompassionDay) " +
                    "VALUES (1700000000000, 25, 90, 1, 2, 1700000000123, 84, 1, 1, 1)"
            )
            old.execSQL(
                "INSERT INTO plants (plantId, species, stage, health, plantedAtUtcMillis, growthPoints, condition) " +
                    "VALUES ('plant', 'fern', 3, 42, 1700000000000, 45, 'RECOVERING')"
            )
            old.execSQL("INSERT INTO app_usage_daily VALUES (1700000000000, 'fake.reader', 5400000, 3, 1700000000123)")
            old.execSQL("INSERT INTO app_limits VALUES ('fake.reader', 60, 1, 1700000000123)")
            old.execSQL(
                "INSERT INTO focus_sessions (sessionId, dayStartUtcMillis, startedAtUtcMillis, plannedMinutes, " +
                    "wasCompleted, plantId, tag, status, actualDurationMs, updatedAtUtcMillis, bootCount) " +
                    "VALUES ('session', 1700000000000, 1700000000000, 25, 0, 'plant', 'READING', 'RUNNING', " +
                    "120000, 1700000120000, 7)"
            )
            old.execSQL("INSERT INTO badges VALUES ('first-recovery', 1700000001000)")
        }
        helper.runMigrationsAndValidate(TEST_DATABASE, 6, true, ScreenGardenDatabase.MIGRATION_5_6).use { db ->
            for (table in listOf(
                "daily_stats",
                "plants",
                "app_usage_daily",
                "app_limits",
                "focus_sessions",
                "badges"
            )) {
                db.query("SELECT COUNT(*) FROM $table").use { rows ->
                    assertTrue(rows.moveToFirst())
                    assertEquals(1, rows.getInt(0))
                }
            }
            db.query("SELECT * FROM garden_history").use { assertEquals(0, it.count) }
            db.query("SELECT * FROM daily_stats").use { rows ->
                assertTrue(rows.moveToFirst())
                assertEquals(1, rows.getInt(rows.getColumnIndexOrThrow("isCompassionDay")))
                assertEquals(84, rows.getInt(rows.getColumnIndexOrThrow("score")))
            }
            db.query("SELECT * FROM focus_sessions").use { rows ->
                assertTrue(rows.moveToFirst())
                assertEquals("RUNNING", rows.getString(rows.getColumnIndexOrThrow("status")))
                assertEquals(120_000L, rows.getLong(rows.getColumnIndexOrThrow("actualDurationMs")))
            }
            db.query("SELECT * FROM plants").use { rows ->
                assertTrue(rows.moveToFirst())
                assertEquals("RECOVERING", rows.getString(rows.getColumnIndexOrThrow("condition")))
                assertEquals(45, rows.getInt(rows.getColumnIndexOrThrow("growthPoints")))
            }
        }
    }

    @Test
    fun allPublishedVersionsCanReach6WithoutLosingHistory() {
        helper.createDatabase(TEST_DATABASE, 1).use { it.execSQL("INSERT INTO badges VALUES ('first-bloom', 1)") }
        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            6,
            true,
            ScreenGardenDatabase.MIGRATION_1_2,
            ScreenGardenDatabase.MIGRATION_2_3,
            ScreenGardenDatabase.MIGRATION_3_4,
            ScreenGardenDatabase.MIGRATION_4_5,
            ScreenGardenDatabase.MIGRATION_5_6
        ).use { db ->
            db.query("SELECT * FROM badges").use { assertEquals(1, it.count) }
            db.query("SELECT * FROM garden_history").use { assertEquals(0, it.count) }
        }
    }

    @Test
    fun migratingFrom4To5PreservesAllDataAndLeavesOldDaysUnmarked() {
        helper.createDatabase(TEST_DATABASE, 4).use { old ->
            old.execSQL(
                "INSERT INTO daily_stats (dayStartUtcMillis, focusMinutes, screenMinutes, sessionsCompleted, plantsGrown, updatedAtUtcMillis, score, usageIsBounded, gardenApplied, bedtimeMinutes) VALUES (1700000000000, 25, 90, 1, 2, 1700000000123, 84, 1, 1, 12)"
            )
            old.execSQL(
                "INSERT INTO plants (plantId, species, stage, health, plantedAtUtcMillis, growthPoints, condition) VALUES ('plant', 'fern', 3, 42, 1700000000000, 45, 'RECOVERING')"
            )
            old.execSQL("INSERT INTO app_usage_daily VALUES (1700000000000, 'fake.reader', 5400000, 3, 1700000000123)")
            old.execSQL("INSERT INTO app_limits VALUES ('fake.reader', 60, 1, 1700000000123)")
            old.execSQL(
                "INSERT INTO focus_sessions (sessionId, dayStartUtcMillis, startedAtUtcMillis, plannedMinutes, wasCompleted, plantId, tag, status, actualDurationMs, updatedAtUtcMillis, bootCount) VALUES ('session', 1700000000000, 1700000000000, 25, 0, 'plant', 'READING', 'RUNNING', 120000, 1700000120000, 7)"
            )
            old.execSQL("INSERT INTO badges VALUES ('first-recovery', 1700000001000)")
        }
        helper.runMigrationsAndValidate(TEST_DATABASE, 5, true, ScreenGardenDatabase.MIGRATION_4_5).use { db ->
            for (table in listOf(
                "daily_stats",
                "plants",
                "app_usage_daily",
                "app_limits",
                "focus_sessions",
                "badges"
            )) {
                db.query("SELECT COUNT(*) FROM $table").use {
                    assertTrue(it.moveToFirst())
                    assertEquals(1, it.getInt(0))
                }
            }
            db.query("SELECT * FROM daily_stats").use {
                assertTrue(it.moveToFirst())
                assertEquals(0, it.getInt(it.getColumnIndexOrThrow("isCompassionDay")))
                assertEquals(12, it.getInt(it.getColumnIndexOrThrow("bedtimeMinutes")))
                assertEquals(84, it.getInt(it.getColumnIndexOrThrow("score")))
                assertEquals(1, it.getInt(it.getColumnIndexOrThrow("gardenApplied")))
            }
            db.query("SELECT * FROM focus_sessions").use {
                assertTrue(it.moveToFirst())
                assertEquals("RUNNING", it.getString(it.getColumnIndexOrThrow("status")))
                assertEquals(120000L, it.getLong(it.getColumnIndexOrThrow("actualDurationMs")))
            }
        }
    }

    @Test
    fun publishedVersion1CanReach5AndKeepEarnedBadges() {
        helper.createDatabase(TEST_DATABASE, 1).use {
            it.execSQL("INSERT INTO badges VALUES ('first-bloom', 1700000001000)")
        }
        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            5,
            true,
            ScreenGardenDatabase.MIGRATION_1_2,
            ScreenGardenDatabase.MIGRATION_2_3,
            ScreenGardenDatabase.MIGRATION_3_4,
            ScreenGardenDatabase.MIGRATION_4_5
        ).use { db ->
            db.query("SELECT * FROM badges").use { assertEquals(1, it.count) }
        }
    }

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

    @Test
    fun migratingFrom1To2KeepsExistingRows() {
        helper.createDatabase(TEST_DATABASE, 1).use { oldDatabase ->
            oldDatabase.execSQL(
                "INSERT INTO daily_stats " +
                    "(dayStartUtcMillis, focusMinutes, screenMinutes, sessionsCompleted, " +
                    "plantsGrown, updatedAtUtcMillis) VALUES (1700000000000, 25, 90, 1, 2, 1700000000123)"
            )
            for (stage in 0..3) {
                oldDatabase.execSQL(
                    "INSERT INTO plants " +
                        "(plantId, species, stage, health, plantedAtUtcMillis, lastFocusAtUtcMillis) " +
                        "VALUES ('plant-$stage', 'fern', $stage, ${if (stage == 3) 40 else 80}, " +
                        "1700000000000, ${if (stage == 0) "NULL" else "1700000001000"})"
                )
            }
            oldDatabase.execSQL(
                "INSERT INTO app_usage_daily " +
                    "(dayStartUtcMillis, packageName, usageMillis, launchCount, updatedAtUtcMillis) " +
                    "VALUES (1700000000000, 'example.app', 5400000, 3, 1700000000123)"
            )
            oldDatabase.execSQL(
                "INSERT INTO app_limits (packageName, dailyLimitMinutes, isEnabled, updatedAtUtcMillis) " +
                    "VALUES ('example.app', 60, 1, 1700000000123)"
            )
            oldDatabase.execSQL(
                "INSERT INTO focus_sessions " +
                    "(sessionId, dayStartUtcMillis, startedAtUtcMillis, endedAtUtcMillis, " +
                    "plannedMinutes, wasCompleted, plantId) " +
                    "VALUES ('session-1', 1700000000000, 1700000000200, 1700000001000, 25, 1, 'plant-1')"
            )
            oldDatabase.execSQL("INSERT INTO badges VALUES ('first-focus', 1700000001000)")
        }

        helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, ScreenGardenDatabase.MIGRATION_1_2).use { migrated ->
            migrated.query("SELECT * FROM daily_stats").use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(1_700_000_000_000L, cursor.getLong(cursor.getColumnIndexOrThrow("dayStartUtcMillis")))
                assertEquals(25, cursor.getInt(cursor.getColumnIndexOrThrow("focusMinutes")))
                assertEquals(90, cursor.getInt(cursor.getColumnIndexOrThrow("screenMinutes")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("sessionsCompleted")))
                assertEquals(2, cursor.getInt(cursor.getColumnIndexOrThrow("plantsGrown")))
                assertEquals(1_700_000_000_123L, cursor.getLong(cursor.getColumnIndexOrThrow("updatedAtUtcMillis")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("score")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("pickups")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("unlocks")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("usageIsBounded")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("gardenApplied")))
            }
            migrated.query("SELECT * FROM plants ORDER BY stage").use { cursor ->
                assertEquals(4, cursor.count)
                val expectedGrowth = listOf(0, 10, 25, 45)
                for (stage in 0..3) {
                    assertTrue(cursor.moveToNext())
                    assertEquals("plant-$stage", cursor.getString(cursor.getColumnIndexOrThrow("plantId")))
                    assertEquals("fern", cursor.getString(cursor.getColumnIndexOrThrow("species")))
                    assertEquals(stage, cursor.getInt(cursor.getColumnIndexOrThrow("stage")))
                    assertEquals(expectedGrowth[stage], cursor.getInt(cursor.getColumnIndexOrThrow("growthPoints")))
                    assertEquals(if (stage == 3) 40 else 80, cursor.getInt(cursor.getColumnIndexOrThrow("health")))
                    assertEquals(
                        if (stage == 3) "WILTING" else "GROWING",
                        cursor.getString(cursor.getColumnIndexOrThrow("condition"))
                    )
                    assertEquals(1_700_000_000_000L, cursor.getLong(cursor.getColumnIndexOrThrow("plantedAtUtcMillis")))
                    if (stage == 0) {
                        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("lastFocusAtUtcMillis")))
                    } else {
                        assertEquals(
                            1_700_000_001_000L,
                            cursor.getLong(cursor.getColumnIndexOrThrow("lastFocusAtUtcMillis"))
                        )
                    }
                }
            }
            migrated.query("SELECT * FROM app_usage_daily").use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("example.app", cursor.getString(cursor.getColumnIndexOrThrow("packageName")))
                assertEquals(5_400_000L, cursor.getLong(cursor.getColumnIndexOrThrow("usageMillis")))
                assertEquals(3, cursor.getInt(cursor.getColumnIndexOrThrow("launchCount")))
                assertEquals(1_700_000_000_123L, cursor.getLong(cursor.getColumnIndexOrThrow("updatedAtUtcMillis")))
            }
            migrated.query("SELECT * FROM app_limits").use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("example.app", cursor.getString(cursor.getColumnIndexOrThrow("packageName")))
                assertEquals(60, cursor.getInt(cursor.getColumnIndexOrThrow("dailyLimitMinutes")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isEnabled")))
            }
            migrated.query("SELECT * FROM focus_sessions").use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("session-1", cursor.getString(cursor.getColumnIndexOrThrow("sessionId")))
                assertEquals("plant-1", cursor.getString(cursor.getColumnIndexOrThrow("plantId")))
                assertEquals(1_700_000_000_200L, cursor.getLong(cursor.getColumnIndexOrThrow("startedAtUtcMillis")))
                assertEquals(1_700_000_001_000L, cursor.getLong(cursor.getColumnIndexOrThrow("endedAtUtcMillis")))
                assertEquals(25, cursor.getInt(cursor.getColumnIndexOrThrow("plannedMinutes")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("wasCompleted")))
            }
            migrated.query("SELECT * FROM badges").use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("first-focus", cursor.getString(cursor.getColumnIndexOrThrow("badgeKey")))
                assertEquals(1_700_000_001_000L, cursor.getLong(cursor.getColumnIndexOrThrow("unlockedAtUtcMillis")))
            }
        }
    }

    @Test
    fun migratingFrom2To3PreservesSessionsAndClosesLegacyTimers() {
        helper.createDatabase(TEST_DATABASE, 2).use { old ->
            for (completed in 0..1) {
                old.execSQL(
                    "INSERT INTO focus_sessions " +
                        "(sessionId, dayStartUtcMillis, startedAtUtcMillis, endedAtUtcMillis, " +
                        "plannedMinutes, wasCompleted, plantId) VALUES " +
                        "('session-$completed', 1700000000000, 1700000000000, " +
                        "${if (completed == 1) "1700000060000" else "NULL"}, 1, $completed, 'plant')"
                )
            }
            old.execSQL("INSERT INTO badges VALUES ('first-bloom', 1700000001000)")
        }
        helper.runMigrationsAndValidate(TEST_DATABASE, 3, true, ScreenGardenDatabase.MIGRATION_2_3).use { migrated ->
            migrated.query("SELECT * FROM focus_sessions ORDER BY sessionId").use { rows ->
                assertEquals(2, rows.count)
                for (completed in 0..1) {
                    assertTrue(rows.moveToNext())
                    assertEquals("OTHER", rows.getString(rows.getColumnIndexOrThrow("tag")))
                    assertEquals(
                        if (completed == 1) "COMPLETED" else "CANCELLED",
                        rows.getString(rows.getColumnIndexOrThrow("status"))
                    )
                    assertEquals(completed * 60_000L, rows.getLong(rows.getColumnIndexOrThrow("actualDurationMs")))
                    assertEquals("plant", rows.getString(rows.getColumnIndexOrThrow("plantId")))
                    assertTrue(!rows.isNull(rows.getColumnIndexOrThrow("endedAtUtcMillis")))
                    assertEquals(-1, rows.getInt(rows.getColumnIndexOrThrow("bootCount")))
                }
            }
            migrated.query("SELECT * FROM badges").use { assertEquals(1, it.count) }
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test.db"
    }

    @Test
    fun migratingFrom3To4KeepsAllTablesAndMarksBedtimeUnknown() {
        helper.createDatabase(TEST_DATABASE, 3).use { old ->
            old.execSQL(
                "INSERT INTO daily_stats (dayStartUtcMillis, focusMinutes, screenMinutes, sessionsCompleted, " +
                    "plantsGrown, updatedAtUtcMillis, score, usageIsBounded, gardenApplied) " +
                    "VALUES (1700000000000, 25, 90, 1, 2, 1700000000123, 84, 1, 1)"
            )
            old.execSQL(
                "INSERT INTO plants (plantId, species, stage, health, plantedAtUtcMillis, growthPoints, condition) " +
                    "VALUES ('plant', 'fern', 3, 42, 1700000000000, 45, 'RECOVERING')"
            )
            old.execSQL("INSERT INTO app_usage_daily VALUES (1700000000000, 'fake.reader', 5400000, 3, 1700000000123)")
            old.execSQL("INSERT INTO app_limits VALUES ('fake.reader', 60, 1, 1700000000123)")
            old.execSQL(
                "INSERT INTO focus_sessions (sessionId, dayStartUtcMillis, startedAtUtcMillis, plannedMinutes, " +
                    "wasCompleted, plantId, tag, status, actualDurationMs, updatedAtUtcMillis, bootCount) " +
                    "VALUES ('session', 1700000000000, 1700000000000, 25, 0, 'plant', 'READING', 'RUNNING', 120000, 1700000120000, 7)"
            )
            old.execSQL("INSERT INTO badges VALUES ('first-recovery', 1700000001000)")
        }
        helper.runMigrationsAndValidate(TEST_DATABASE, 4, true, ScreenGardenDatabase.MIGRATION_3_4).use { migrated ->
            for (table in listOf(
                "daily_stats",
                "plants",
                "app_usage_daily",
                "app_limits",
                "focus_sessions",
                "badges"
            )) {
                migrated.query("SELECT COUNT(*) FROM $table").use { rows ->
                    assertTrue(rows.moveToFirst())
                    assertEquals(1, rows.getInt(0))
                }
            }
            migrated.query("SELECT * FROM daily_stats").use { rows ->
                assertTrue(rows.moveToFirst())
                assertTrue(rows.isNull(rows.getColumnIndexOrThrow("bedtimeMinutes")))
                assertEquals(84, rows.getInt(rows.getColumnIndexOrThrow("score")))
                assertEquals(1, rows.getInt(rows.getColumnIndexOrThrow("gardenApplied")))
                assertEquals(25, rows.getInt(rows.getColumnIndexOrThrow("focusMinutes")))
            }
            migrated.query("SELECT * FROM focus_sessions").use { rows ->
                assertTrue(rows.moveToFirst())
                assertEquals("RUNNING", rows.getString(rows.getColumnIndexOrThrow("status")))
                assertEquals(120_000L, rows.getLong(rows.getColumnIndexOrThrow("actualDurationMs")))
            }
            migrated.query("SELECT * FROM plants").use { rows ->
                assertTrue(rows.moveToFirst())
                assertEquals("RECOVERING", rows.getString(rows.getColumnIndexOrThrow("condition")))
                assertEquals(45, rows.getInt(rows.getColumnIndexOrThrow("growthPoints")))
            }
        }
    }

    @Test
    fun allPublishedVersionsCanReach4WithoutDestructiveFallback() {
        helper.createDatabase(TEST_DATABASE, 1).use {
            it.execSQL("INSERT INTO badges VALUES ('first-bloom', 1700000001000)")
        }
        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            4,
            true,
            ScreenGardenDatabase.MIGRATION_1_2,
            ScreenGardenDatabase.MIGRATION_2_3,
            ScreenGardenDatabase.MIGRATION_3_4
        ).use { migrated ->
            migrated.query("SELECT * FROM badges").use { assertEquals(1, it.count) }
        }
    }
}
