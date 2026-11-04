// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.sync

/** What a [DailyUsageSync.run] call did. */
sealed interface DailyUsageSyncResult {

    /**
     * The days that were read and written, newest first.
     *
     * One entry means only today was synced; seven means the first-run backfill
     * ran. Callers do not need the count, but the worker logs it and the tests
     * assert it, which is cheaper than making the class report side effects.
     */
    data class Synced(val days: List<Long>) : DailyUsageSyncResult

    /**
     * Usage access is not granted, so nothing was read or written.
     *
     * A normal state, not a failure (D46), and deliberately not retried: no amount
     * of retrying grants a permission the user has to give in Settings.
     */
    data object MissingUsageAccess : DailyUsageSyncResult
}
