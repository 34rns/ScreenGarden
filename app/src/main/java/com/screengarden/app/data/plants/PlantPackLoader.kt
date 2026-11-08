// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.plants

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.screengarden.app.domain.garden.PlantPack
import com.screengarden.app.domain.garden.PlantStage
import com.screengarden.app.domain.garden.PlantUnlock
import com.screengarden.app.domain.garden.Season
import java.io.IOException
import org.json.JSONObject
import org.json.JSONTokener

data class LoadedPlantPack(val metadata: PlantPack, val images: Map<PlantStage, ImageBitmap>)
data class PlantPackCatalog(val packs: List<LoadedPlantPack>, val rejected: Map<String, String>)

/** Small, bounded reads; a pack can never escape its asset directory. */
interface PlantPackSource {
    fun directories(): List<String>
    fun read(directory: String, file: String, maximumBytes: Int): ByteArray
}

class AssetPlantPackSource(private val assets: AssetManager) : PlantPackSource {
    override fun directories(): List<String> =
        // Robolectric's test APK listing includes nested paths; Android lists immediate children.
        assets.list("plants")?.map { it.substringBefore('/') }?.distinct().orEmpty()

    override fun read(directory: String, file: String, maximumBytes: Int): ByteArray =
        assets.open("plants/$directory/$file").use { stream ->
            val result = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                require(result.size() + count <= maximumBytes) { "Asset exceeds size limit" }
                result.write(buffer, 0, count)
            }
            result.toByteArray()
        }
}

/** Invalid/future/unlicensed packs are isolated; callers retain vector art as a fallback. */
class PlantPackLoader(private val source: PlantPackSource) {
    fun load(): PlantPackCatalog {
        val accepted = mutableListOf<LoadedPlantPack>()
        val rejected = linkedMapOf<String, String>()
        val directories = try {
            source.directories().sorted()
        } catch (_: IOException) {
            return PlantPackCatalog(emptyList(), mapOf("plants" to "Assets unavailable"))
        }
        for ((index, directory) in directories.withIndex()) {
            try {
                require(index < MAX_PACKS) { "Catalog exceeds pack limit" }
                require(ID.matches(directory)) { "Unsafe directory" }
                val metadata = parse(source.read(directory, "plant.json", MAX_JSON_BYTES), directory)
                val images = metadata.stageImages.mapValues { (_, filename) ->
                    decode(source.read(directory, filename, MAX_IMAGE_BYTES))
                }
                accepted += LoadedPlantPack(metadata, images)
            } catch (_: Exception) {
                // Fixed reasons reveal neither document contents nor private file paths.
                rejected[directory] = "Invalid metadata, license, schema or stage image"
            }
        }
        return PlantPackCatalog(accepted, rejected)
    }

    private fun decode(bytes: ByteArray): ImageBitmap {
        require(bytes.size in 8..MAX_IMAGE_BYTES && bytes.take(8) == PNG_SIGNATURE) {
            "Only bounded PNG artwork is supported"
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth in 1..MAX_DIMENSION && bounds.outHeight in 1..MAX_DIMENSION) {
            "Artwork dimensions exceed bounds"
        }
        return requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)).asImageBitmap()
    }

    companion object {
        const val MAX_PACKS: Int = 16
        const val MAX_JSON_BYTES: Int = 16_384
        const val MAX_IMAGE_BYTES: Int = 1_048_576
        const val MAX_DIMENSION: Int = 512
        private val ID = Regex("[a-z][a-z0-9_-]{0,39}")
        private val IMAGE = Regex("[a-z][a-z0-9_-]{0,63}\\.png")
        private val LICENSES = setOf("CC0-1.0", "CC-BY-4.0", "CC-BY-SA-4.0")
        private val PNG_SIGNATURE = listOf(137, 80, 78, 71, 13, 10, 26, 10).map { it.toByte() }

        /** Schema v1 is intentionally strict, including JSON numeric/string types. */
        fun parse(bytes: ByteArray, expectedId: String): PlantPack {
            require(bytes.size <= MAX_JSON_BYTES && ID.matches(expectedId))
            val tokener = JSONTokener(bytes.toString(Charsets.UTF_8))
            val json = tokener.nextValue() as? JSONObject ?: error("Expected an object")
            require(tokener.nextClean() == '\u0000') { "Trailing JSON content" }
            require(
                json.keys().asSequence().toSet() == setOf(
                    "schemaVersion",
                    "id",
                    "name",
                    "description",
                    "author",
                    "license",
                    "unlock",
                    "stageImages"
                )
            )
            require(integer(json, "schemaVersion", 1..1) == 1)
            val id = text(json, "id", 40)
            require(id == expectedId)
            val license = text(json, "license", 32)
            require(license in LICENSES) { "Artwork license is not supported" }
            val unlock = json.getJSONObject("unlock")
            val rule = when (text(unlock, "type", 16)) {
                "default" -> {
                    require(unlock.length() == 1)
                    PlantUnlock.Default
                }
                "streak" -> {
                    require(unlock.length() == 2)
                    PlantUnlock.Streak(integer(unlock, "days", 1..365))
                }
                "focus" -> {
                    require(unlock.length() == 2)
                    PlantUnlock.Focus(integer(unlock, "completions", 1..10_000))
                }
                "season" -> {
                    require(unlock.length() == 2)
                    PlantUnlock.Seasonal(Season.valueOf(text(unlock, "season", 16)))
                }
                else -> error("Unknown unlock rule")
            }
            val stages = json.getJSONObject("stageImages")
            require(stages.keys().asSequence().toSet() == PlantStage.entries.map { it.name }.toSet())
            val images = PlantStage.entries.associateWith { stage ->
                text(stages, stage.name, 68).also { require(IMAGE.matches(it)) { "Unsafe image path" } }
            }
            return PlantPack(
                id,
                text(json, "name", 80),
                text(json, "description", 500),
                text(json, "author", 120),
                license,
                rule,
                images
            )
        }

        private fun text(json: JSONObject, key: String, maximum: Int): String {
            val value = json.get(key)
            require(value is String && value.isNotBlank() && value.length <= maximum)
            require(value.none { it.isISOControl() })
            return value
        }

        private fun integer(json: JSONObject, key: String, range: IntRange): Int {
            val value = json.get(key)
            require(value is Int && value in range)
            return value
        }
    }
}
