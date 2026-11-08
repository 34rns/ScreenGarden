// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.plants

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.screengarden.app.domain.garden.PlantStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android AssetManager and PNG decoding, independent of any private usage data. */
@RunWith(AndroidJUnit4::class)
class PlantPackAndroidTest {
    @Test fun allBundledPacksLoadOnAndroid() {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val result = PlantPackLoader(AssetPlantPackSource(assets)).load()
        assertTrue(result.rejected.isEmpty())
        assertEquals(setOf("fern", "flower", "bonsai", "cactus"), result.packs.map { it.metadata.id }.toSet())
        result.packs.forEach { pack ->
            assertEquals(PlantStage.entries.toSet(), pack.images.keys)
            assertTrue(pack.images.values.all { it.width == 256 && it.height == 256 })
        }
    }
}
