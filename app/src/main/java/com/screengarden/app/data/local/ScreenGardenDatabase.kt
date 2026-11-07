// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.screengarden.app.data.local.dao.AppLimitDao
import com.screengarden.app.data.local.dao.AppUsageDailyDao
import com.screengarden.app.data.local.dao.BadgeDao
import com.screengarden.app.data.local.dao.DailyStatDao
import com.screengarden.app.data.local.dao.FocusSessionDao
import com.screengarden.app.data.local.dao.PlantDao
import com.screengarden.app.data.local.entity.AppLimit
import com.screengarden.app.data.local.entity.AppUsageDaily
import com.screengarden.app.data.local.entity.Badge
import com.screengarden.app.data.local.entity.DailyStat
import com.screengarden.app.data.local.entity.FocusSession
import com.screengarden.app.data.local.entity.Plant

/** Bumped together with [DATABASE_VERSION] when the schema changes (D22). */
private const val DATABASE_VERSION = 3

/**
 * The app's single Room database: everything ScreenGarden knows, stored on device.
 *
 * DECISION (D22): schema export is ON and the exported JSON lives in
 * `app/schemas/`. Without the history, a migration can never be tested, so the
 * first migration would be written blind. The schema files are committed.
 *
 * DECISION (D23): no destructive fallback. `fallbackToDestructiveMigration` is
 * deliberately NOT used: losing a user's history silently would betray the
 * "your data stays yours" promise. Every version bump ships a real [Migration]
 * and a test in ScreenGardenDatabaseMigrationTest.
 */
@Database(
    entities = [
        DailyStat::class,
        AppUsageDaily::class,
        AppLimit::class,
        Plant::class,
        FocusSession::class,
        Badge::class
    ],
    version = DATABASE_VERSION,
    exportSchema = true
)
abstract class ScreenGardenDatabase : RoomDatabase() {

    abstract fun dailyStatDao(): DailyStatDao

    abstract fun appUsageDailyDao(): AppUsageDailyDao

    abstract fun appLimitDao(): AppLimitDao

    abstract fun plantDao(): PlantDao

    abstract fun focusSessionDao(): FocusSessionDao

    abstract fun badgeDao(): BadgeDao

    companion object {
        /** Mirrors [DATABASE_VERSION]; Room reads the literal, tests read this. */
        const val VERSION: Int = DATABASE_VERSION

        const val NAME: String = "screengarden.db"

        /** Preserves every v1 row and makes old usage explicitly untrusted. */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_stats ADD COLUMN score INTEGER")
                db.execSQL("ALTER TABLE daily_stats ADD COLUMN pickups INTEGER")
                db.execSQL("ALTER TABLE daily_stats ADD COLUMN unlocks INTEGER")
                db.execSQL("ALTER TABLE daily_stats ADD COLUMN usageIsBounded INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_stats ADD COLUMN gardenApplied INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE plants ADD COLUMN growthPoints INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE plants ADD COLUMN condition TEXT NOT NULL DEFAULT 'GROWING'")
                db.execSQL(
                    "UPDATE plants SET growthPoints = CASE " +
                        "WHEN stage >= 3 THEN 45 WHEN stage = 2 THEN 25 WHEN stage = 1 THEN 10 ELSE 0 END, " +
                        "condition = CASE WHEN health < 60 THEN 'WILTING' ELSE 'GROWING' END"
                )
            }
        }

        /** Append migrations here; never rewrite an already shipped migration. */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN tag TEXT NOT NULL DEFAULT 'OTHER'")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN status TEXT NOT NULL DEFAULT 'RUNNING'")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN actualDurationMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN updatedAtUtcMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN bootCount INTEGER NOT NULL DEFAULT -1")
                // Legacy rows had no reliable timer checkpoints; never resurrect them.
                db.execSQL(
                    "UPDATE focus_sessions SET status = CASE WHEN wasCompleted = 1 THEN 'COMPLETED' " +
                        "ELSE 'CANCELLED' END, " +
                        "actualDurationMs = MAX(0, MIN(plannedMinutes * 60000, " +
                        "COALESCE(endedAtUtcMillis, startedAtUtcMillis) - startedAtUtcMillis)), " +
                        "updatedAtUtcMillis = COALESCE(endedAtUtcMillis, startedAtUtcMillis), " +
                        "endedAtUtcMillis = COALESCE(endedAtUtcMillis, startedAtUtcMillis)"
                )
            }
        }

        private val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

        /** Builds the on-disk database. The DI container calls this lazily (D1). */
        fun build(context: Context): ScreenGardenDatabase =
            Room.databaseBuilder(context, ScreenGardenDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
