// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.notifications

/** A crossed milestone; the local-day key is kept separately by the delivery ledger. */
data class Nudge(val threshold: Int, val packageName: String? = null) {
    val key: String get() = "${packageName?.let { "app:$it" } ?: "daily"}:$threshold"
}

/** Threshold arithmetic without Android services or a clock. */
object NudgePolicy {
    fun crossed(usedMs: Long, goalMs: Long, packageName: String? = null): List<Nudge> {
        if (usedMs < 0 || goalMs <= 0) return emptyList()
        val percentage = usedMs.toDouble() / goalMs.toDouble() * 100.0
        return listOf(50, 80, 100).filter { percentage >= it }.map { Nudge(it, packageName) }
    }
}
