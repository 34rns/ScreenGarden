// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.focus

import com.screengarden.app.data.usage.DefaultUsageRepository

/** Unknown evidence never causes a soft fail. No distraction package names are stored. */
class FocusUseReader(private val usage: DefaultUsageRepository, private val ignoredPackages: Set<String>) {
    fun otherAppUsageMillis(startUtcMillis: Long, nowUtcMillis: Long): Long? =
        usage.readWindow(startUtcMillis, nowUtcMillis, screenOnAtStart = true)
            ?.takeIf { it.usageIsBounded }
            ?.apps?.filter { it.packageName !in ignoredPackages }?.sumOf { it.usageMillis }
}
