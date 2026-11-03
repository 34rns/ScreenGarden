// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.usage

import java.time.ZoneId

/**
 * Reads screen time from the operating system.
 *
 * Read-only by design: P05 reports what the system knows, and persisting it into
 * `app_usage_daily` is the daily sync's job (P06), so nothing here writes to Room
 * and a repeated read is always safe.
 *
 * Every method can be called without Usage access; the answer is then
 * [DayUsageResult.MissingPermission] rather than an exception.
 */
interface UsageRepository {

    /** True when the user has granted Usage access to this app. */
    suspend fun isUsageAccessGranted(): Boolean

    /**
     * Screen time, per-app usage, pickups and unlocks for the local day that starts
     * at [dayStartUtcMillis].
     *
     * The window is `[dayStartUtcMillis, nextDayStart(dayStartUtcMillis))`, computed
     * by [com.screengarden.app.core.time.LocalDay] so a DST day is 23 or 25 hours
     * long instead of silently reusing 24 (H13, D20).
     */
    suspend fun readDay(dayStartUtcMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): DayUsageResult
}
