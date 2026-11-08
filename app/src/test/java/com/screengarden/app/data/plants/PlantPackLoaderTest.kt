// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.plants

import androidx.test.core.app.ApplicationProvider
import com.screengarden.app.domain.garden.PlantStage
import com.screengarden.app.domain.garden.PlantUnlock
import com.screengarden.app.domain.garden.Season
import com.screengarden.app.domain.garden.chooseSeedSpecies
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PlantPackLoaderTest {
    private val assets = ApplicationProvider.getApplicationContext<android.content.Context>().assets
    private fun json(): JSONObject = JSONObject(
        """{"schemaVersion":1,"id":"fern","name":"Fern","description":"A calm fern.",
        "author":"ScreenGarden contributors","license":"CC-BY-SA-4.0","unlock":{"type":"default"},
        "stageImages":{"SEED":"seed.png","SPROUT":"sprout.png","BUD":"bud.png","BLOOM":"bloom.png"}}"""
    )
    private fun parse(json: JSONObject) = PlantPackLoader.parse(json.toString().toByteArray(), "fern")
    private fun rejected(json: JSONObject) = assertTrue(runCatching { parse(json) }.isFailure)

    @Test fun everyBundledPackHasFourReadableStagesAndAnExplicitLicense() {
        val catalog = PlantPackLoader(AssetPlantPackSource(assets)).load()
        assertEquals(emptyMap<String, String>(), catalog.rejected)
        assertEquals(setOf("fern", "flower", "bonsai", "cactus"), catalog.packs.map { it.metadata.id }.toSet())
        catalog.packs.forEach {
            assertEquals(PlantStage.entries.toSet(), it.images.keys)
            assertEquals("CC-BY-SA-4.0", it.metadata.license)
            assertTrue(it.images.values.all { image -> image.width == 256 && image.height == 256 })
        }
    }

    @Test fun rejectsFutureVersionsMissingFieldsWrongTypesAndUnknownKeys() {
        rejected(json().put("schemaVersion", 2))
        rejected(json().put("schemaVersion", "1"))
        rejected(json().put("schemaVersion", 1.5))
        rejected(json().apply { remove("author") })
        rejected(json().put("name", true))
        rejected(json().put("unexpected", 0))
        assertTrue(
            runCatching {
                PlantPackLoader.parse((json().toString() + " trailing").toByteArray(), "fern")
            }.isFailure
        )
    }

    @Test fun rejectsMissingOrRestrictedLicensesAndUnsafePaths() {
        for (license in listOf("", "proprietary", "CC-BY-NC-4.0", "CC-BY-ND-4.0")) {
            rejected(json().put("license", license))
        }
        for (name in listOf("../seed.png", "/seed.png", "sub/seed.png", "sub\\seed.png", "https://art.png")) {
            rejected(json().apply { getJSONObject("stageImages").put("SEED", name) })
        }
        rejected(json().put("id", "other"))
        rejected(json().apply { getJSONObject("stageImages").remove("BUD") })
    }

    @Test fun validatesAllUnlockTypesAndTheirBounds() {
        assertEquals(PlantUnlock.Default, parse(json()).unlock)
        assertEquals(
            PlantUnlock.Streak(7),
            parse(json().put("unlock", JSONObject("""{"type":"streak","days":7}"""))).unlock
        )
        assertEquals(
            PlantUnlock.Focus(5),
            parse(json().put("unlock", JSONObject("""{"type":"focus","completions":5}"""))).unlock
        )
        assertEquals(
            PlantUnlock.Seasonal(Season.SUMMER),
            parse(json().put("unlock", JSONObject("""{"type":"season","season":"SUMMER"}"""))).unlock
        )
        for (rule in listOf(
            """{"type":"streak","days":0}""",
            """{"type":"streak","days":"7"}""",
            """{"type":"focus","completions":-1}""",
            """{"type":"season","season":"SPRINGTIME"}""",
            """{"type":"default","days":2}""",
            """{"type":"unknown"}"""
        )) {
            rejected(json().put("unlock", JSONObject(rule)))
        }
    }

    @Test fun corruptOrMissingArtworkDoesNotHideValidPacks() {
        val real = AssetPlantPackSource(assets)
        val source = object : PlantPackSource {
            override fun directories(): List<String> = real.directories() + "../escape"
            override fun read(directory: String, file: String, maximumBytes: Int): ByteArray =
                if (directory == "flower" && file.endsWith(".png")) {
                    byteArrayOf(1, 2, 3)
                } else {
                    real.read(directory, file, maximumBytes)
                }
        }
        val catalog = PlantPackLoader(source).load()
        assertEquals(3, catalog.packs.size)
        assertEquals(setOf("flower", "../escape"), catalog.rejected.keys)
    }

    @Test fun unlockBoundariesAndSeasonChangesOnlyAffectEligibleSeeds() {
        assertTrue(!PlantUnlock.Streak(7).isUnlocked(6, 50, Season.SUMMER))
        assertTrue(PlantUnlock.Streak(7).isUnlocked(7, 0, Season.WINTER))
        assertTrue(!PlantUnlock.Focus(5).isUnlocked(30, 4, Season.SUMMER))
        assertTrue(PlantUnlock.Focus(5).isUnlocked(0, 5, Season.WINTER))
        assertTrue(!PlantUnlock.Seasonal(Season.SUMMER).isUnlocked(30, 50, Season.WINTER))
        val packs = PlantPackLoader(AssetPlantPackSource(assets)).load().packs.map { it.metadata }
        assertEquals("fern", chooseSeedSpecies(packs, 0, 0, 0, Season.WINTER))
        assertEquals("fern", chooseSeedSpecies(packs, 4, 0, 0, Season.WINTER))
        assertEquals("bonsai", chooseSeedSpecies(packs, 3, 3, 0, Season.WINTER))
        assertEquals("flower", chooseSeedSpecies(packs, 1, 0, 5, Season.WINTER))
        assertEquals("fern", chooseSeedSpecies(emptyList(), 100, 100, 100, Season.SUMMER))
    }

    @Test fun oversizedMetadataAndImagesAreRejectedBeforeLargeDecoding() {
        assertTrue(
            runCatching {
                PlantPackLoader.parse(ByteArray(PlantPackLoader.MAX_JSON_BYTES + 1), "fern")
            }.isFailure
        )
        val real = AssetPlantPackSource(assets)
        val source = object : PlantPackSource {
            override fun directories(): List<String> = listOf("fern")
            override fun read(directory: String, file: String, maximumBytes: Int): ByteArray {
                if (file == "plant.json") return real.read(directory, file, maximumBytes)
                val bitmap = android.graphics.Bitmap.createBitmap(513, 1, android.graphics.Bitmap.Config.ARGB_8888)
                return java.io.ByteArrayOutputStream().apply {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, this)
                }.toByteArray()
            }
        }
        val result = PlantPackLoader(source).load()
        assertTrue(result.packs.isEmpty())
        assertEquals(setOf("fern"), result.rejected.keys)
    }
}
