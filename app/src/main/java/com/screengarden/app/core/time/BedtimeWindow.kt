// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.core.time

import java.time.LocalTime

/** Start-inclusive, end-exclusive local quiet hours; an equal pair disables the window. */
data class BedtimeWindow(val start: LocalTime, val end: LocalTime) {
    fun contains(time: LocalTime): Boolean = when {
        start == end -> false
        start < end -> time >= start && time < end
        else -> time >= start || time < end
    }
}
