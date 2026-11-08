// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.domain.help

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryGuideTest {
    @Test
    fun matchingManufacturerIsFirstWithoutDroppingOtherGuides() {
        val expected = mapOf(
            " SAMSUNG " to BatteryGuide.SAMSUNG,
            "Xiaomi" to BatteryGuide.XIAOMI,
            "OPPO" to BatteryGuide.OPPO,
            "vivo" to BatteryGuide.VIVO,
            "HUAWEI" to BatteryGuide.HUAWEI,
            "OnePlus" to BatteryGuide.ONEPLUS,
            "Google" to BatteryGuide.STOCK,
            "unknown" to BatteryGuide.STOCK
        )
        expected.forEach { (manufacturer, first) ->
            val guides = BatteryGuide.ordered(manufacturer)
            assertEquals(first, guides.first())
            assertEquals(7, guides.distinct().size)
        }
    }
}
