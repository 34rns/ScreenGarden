// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.garden

import kotlinx.coroutines.flow.Flow

/** Domain storage boundary; all day keys are supplied by the caller's LocalDay. */
interface GardenStore {

    fun observeSnapshot(fromDayStartUtcMillis: Long, toDayStartUtcMillis: Long): Flow<GardenSnapshot>

    /** Stores a score only while its measured row is unchanged and unapplied. */
    suspend fun recordScore(dayStartUtcMillis: Long, score: Int?, expectedUpdatedAtUtcMillis: Long): Boolean

    /** Atomically rewards a bounded, scored completed day once; false means no change. */
    suspend fun advanceCompletedDay(day: GardenDay): Boolean
}
