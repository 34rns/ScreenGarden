// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.help

import java.util.Locale

/** Brand names select help only; no device identifier is saved or sent. */
enum class BatteryGuide {
    SAMSUNG,
    XIAOMI,
    OPPO,
    VIVO,
    HUAWEI,
    ONEPLUS,
    STOCK;

    companion object {
        fun ordered(manufacturer: String): List<BatteryGuide> {
            val brand = manufacturer.trim().uppercase(Locale.ROOT)
            val first = entries.firstOrNull { it.name == brand } ?: STOCK
            return listOf(first) + entries.filter { it != first }
        }
    }
}
