// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
private const val DATABASE_VERSION = 1

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

        /** Empty until version 2 exists; append a migration here, never edit one. */
        private val MIGRATIONS: Array<Migration> = emptyArray()

        /** Builds the on-disk database. The DI container calls this lazily (D1). */
        fun build(context: Context): ScreenGardenDatabase =
            Room.databaseBuilder(context, ScreenGardenDatabase::class.java, NAME)
                .apply {
                    if (MIGRATIONS.isNotEmpty()) {
                        addMigrations(*MIGRATIONS)
                    }
                }
                .build()
    }
}
