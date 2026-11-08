// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.notifications

import android.content.Context
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Delivery flags are operational state, separate from user preferences and Room history. */
interface NudgeLedger {
    suspend fun delivered(dayStartUtcMillis: Long): Set<String>
    suspend fun claim(dayStartUtcMillis: Long, keys: Set<String>): Boolean
}

/** A small app-private ledger; no new user-setting key or database schema. */
class PreferencesNudgeLedger(context: Context) : NudgeLedger {
    private val flags = context.applicationContext.getSharedPreferences("nudge_delivery_flags", Context.MODE_PRIVATE)

    override suspend fun delivered(dayStartUtcMillis: Long): Set<String> = lock.withLock {
        flags.getStringSet(dayStartUtcMillis.toString(), emptySet()).orEmpty().toSet()
    }

    override suspend fun claim(dayStartUtcMillis: Long, keys: Set<String>): Boolean = lock.withLock {
        val dayKey = dayStartUtcMillis.toString()
        val old = flags.getStringSet(dayKey, emptySet()).orEmpty().toSet()
        if (keys.all { it in old }) return@withLock false
        // Persist before posting: a process gap may miss a nudge, but must not repeat it.
        if (!flags.edit().putStringSet(dayKey, old + keys).commit()) throw IOException("Nudge flags were not saved")
        true
    }

    private companion object {
        val lock = Mutex()
    }
}
