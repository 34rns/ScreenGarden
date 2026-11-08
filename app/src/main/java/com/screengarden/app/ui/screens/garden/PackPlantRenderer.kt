// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantStage
import com.screengarden.app.ui.theme.OriginalPackLeaf
import kotlin.math.min
import kotlin.math.roundToInt

/** Already-decoded art only. No storage reads or eligibility changes occur while drawing. */
class PackPlantRenderer(private val images: Map<String, Map<PlantStage, ImageBitmap>>) : PlantRenderer {
    override fun draw(scope: DrawScope, plant: Plant, palette: PlantPalette, visuals: PlantVisuals) {
        val image = images[plant.species]?.get(plant.stage)
        if (image == null) {
            VectorPlantRenderer.draw(scope, plant, palette, visuals)
            return
        }
        with(scope) {
            val fit = min(size.width / image.width, size.height / image.height)
            val width = (image.width * fit).roundToInt().coerceAtLeast(1)
            val height = (image.height * fit).roundToInt().coerceAtLeast(1)
            val x = ((size.width - width) / 2).roundToInt()
            val y = (size.height - height).roundToInt()
            val matrix = ColorMatrix().apply { setToSaturation(1f - visuals.wilt.coerceIn(0f, 1f) * 0.75f) }
            val bundled = plant.species in setOf("fern", "flower", "bonsai", "cactus")
            if (bundled) {
                // Original packs use OriginalPackLeaf; adapt their leaf hue to the selected theme/accent.
                val gains = floatArrayOf(
                    palette.leaf.red / OriginalPackLeaf.red,
                    palette.leaf.green / OriginalPackLeaf.green,
                    palette.leaf.blue / OriginalPackLeaf.blue
                )
                for (row in 0..2) for (column in 0..4) matrix.values[row * 5 + column] *= gains[row]
            }
            val filter = if (bundled && plant.stage == PlantStage.SEED) {
                ColorFilter.tint(palette.soil)
            } else {
                ColorFilter.colorMatrix(matrix)
            }
            val targetGrowth = when (plant.stage) {
                PlantStage.SEED -> 0f
                PlantStage.SPROUT -> 0.5f
                PlantStage.BUD -> 0.78f
                PlantStage.BLOOM -> 1f
            }
            val animatedScale = if (targetGrowth == 0f) {
                1f
            } else {
                (visuals.growth / targetGrowth).coerceIn(0.01f, 1.25f)
            }
            withTransform({
                val anchor = Offset(size.width / 2f, size.height - height * 0.13f)
                rotate(visuals.swayDegrees + visuals.wilt * 6f, anchor)
                scale(animatedScale, animatedScale, anchor)
            }) {
                val bloom = visuals.bloom.coerceIn(0f, 1f)
                val bud = if (plant.stage == PlantStage.BLOOM) images[plant.species]?.get(PlantStage.BUD) else null
                if (bud != null && bloom < 1f) {
                    drawImage(
                        bud,
                        dstOffset = IntOffset(x, y),
                        dstSize = IntSize(width, height),
                        alpha = 1f - bloom,
                        colorFilter = filter
                    )
                }
                drawImage(
                    image,
                    dstOffset = IntOffset(x, y),
                    dstSize = IntSize(width, height),
                    alpha = if (bud == null) 1f else bloom,
                    colorFilter = filter
                )
            }
        }
    }
}
