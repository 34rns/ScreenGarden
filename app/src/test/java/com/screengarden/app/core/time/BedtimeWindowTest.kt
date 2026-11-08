// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.core.time

import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BedtimeWindowTest {
    private val night = BedtimeWindow(LocalTime.of(22, 30), LocalTime.of(7, 0))

    @Test fun midnightWindowHonorsBothBoundaries() {
        assertTrue(night.contains(LocalTime.of(22, 30)))
        assertTrue(night.contains(LocalTime.of(0, 0)))
        assertTrue(night.contains(LocalTime.of(6, 59)))
        assertFalse(night.contains(LocalTime.of(7, 0)))
        assertFalse(night.contains(LocalTime.of(22, 29)))
    }

    @Test fun daytimeWindowAndDisabledWindowDoNotWrap() {
        val daytime = BedtimeWindow(LocalTime.of(9, 0), LocalTime.of(17, 0))
        assertTrue(daytime.contains(LocalTime.of(9, 0)))
        assertFalse(daytime.contains(LocalTime.of(17, 0)))
        assertFalse(daytime.contains(LocalTime.of(0, 0)))
        assertFalse(BedtimeWindow(LocalTime.NOON, LocalTime.NOON).contains(LocalTime.NOON))
    }
}
