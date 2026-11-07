// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantStage
import java.util.Locale
import kotlin.math.min

/** Semantic theme colors supplied by the screen, including light, dark and AMOLED variants. */
data class PlantPalette(
    val leaf: Color,
    val mutedLeaf: Color,
    val stem: Color,
    val bloom: Color,
    val bloomCenter: Color,
    val soil: Color
)

/**
 * Replaceable art boundary. A renderer draws within [DrawScope.size] without owning taps,
 * accessibility, storage or time. A validated plant pack can supply an image renderer here.
 */
fun interface PlantRenderer {
    fun draw(scope: DrawScope, plant: Plant, palette: PlantPalette, visuals: PlantVisuals)
}

/** Drawing inputs only; time, lifecycle and animation state belong to the composable. */
data class PlantVisuals(val growth: Float, val bloom: Float, val wilt: Float, val swayDegrees: Float = 0f)

/** Override at the garden's composition boundary when a plant pack is selected. */
val LocalPlantRenderer = staticCompositionLocalOf<PlantRenderer> { VectorPlantRenderer }

/** Original offline vector art; unknown species receive the same safe drawing as a fern. */
object VectorPlantRenderer : PlantRenderer {

    override fun draw(scope: DrawScope, plant: Plant, palette: PlantPalette, visuals: PlantVisuals) {
        with(scope) {
            val unit = min(size.width, size.height) / 100f
            withTransform({
                translate((size.width - unit * 100f) / 2f, size.height - unit * 100f)
                scale(unit, unit, pivot = Offset.Zero)
            }) {
                drawOval(palette.soil.copy(alpha = 0.16f), Offset(24f, 85f), Size(52f, 8f))
                if (visuals.growth < 0.35f) {
                    drawOval(
                        palette.soil.copy(alpha = (1f - visuals.growth / 0.35f).coerceIn(0f, 1f)),
                        Offset(45f, 79f),
                        Size(10f, 7f)
                    )
                    drawLine(palette.mutedLeaf, Offset(50f, 79f), Offset(53f, 75f), 2f, StrokeCap.Round)
                }
                if (visuals.growth > 0f) {
                    val leaf = lerp(palette.leaf, palette.mutedLeaf, visuals.wilt * 0.75f)
                    withTransform({
                        rotate(visuals.swayDegrees + visuals.wilt * 6f, pivot = Offset(50f, 87f))
                        scale(visuals.growth, visuals.growth, pivot = Offset(50f, 87f))
                    }) {
                        val droop = visuals.wilt * 11f
                        when (plant.species.lowercase(Locale.ROOT)) {
                            "flower" -> drawFlower(plant, palette, leaf, droop, visuals.bloom)
                            "bonsai" -> drawBonsai(palette, leaf, droop)
                            "cactus" -> drawCactus(plant, palette, leaf)
                            else -> drawFern(palette, leaf, droop)
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawFlower(plant: Plant, palette: PlantPalette, leaf: Color, droop: Float, bloom: Float) {
    val tip = Offset(50f + droop, 28f + droop)
    val stem = Path().apply {
        moveTo(50f, 87f)
        quadraticTo(44f, 55f, tip.x, tip.y)
    }
    drawPath(stem, palette.stem, style = Stroke(3f, cap = StrokeCap.Round))
    drawLeaf(Offset(48f, 66f), Offset(28f, 49f), leaf)
    drawLeaf(Offset(49f, 73f), Offset(71f, 57f), leaf)
    if (bloom > 0f) {
        val petals = listOf(Offset(-10f, 0f), Offset(10f, 0f), Offset(0f, -10f), Offset(0f, 10f))
        petals.forEach { drawCircle(palette.bloom, 4f + 5f * bloom, tip + it * bloom) }
        drawCircle(palette.bloomCenter, 5f * bloom, tip)
    } else if (plant.stage == PlantStage.BUD) {
        drawOval(palette.bloom, tip - Offset(6f, 9f), Size(12f, 18f))
    } else {
        drawLeaf(tip, tip + Offset(14f, -12f), leaf)
    }
}

private fun DrawScope.drawBonsai(palette: PlantPalette, leaf: Color, droop: Float) {
    val trunk = Path().apply {
        moveTo(48f, 86f)
        cubicTo(61f, 66f, 40f, 52f, 52f, 31f)
    }
    drawPath(trunk, palette.stem, style = Stroke(7f, cap = StrokeCap.Round))
    drawLine(palette.stem, Offset(50f, 57f), Offset(31f, 46f + droop), 4f, StrokeCap.Round)
    drawLine(palette.stem, Offset(51f, 45f), Offset(74f, 39f + droop), 4f, StrokeCap.Round)
    drawOval(leaf, Offset(14f, 32f + droop), Size(38f, 20f))
    drawOval(leaf, Offset(56f, 25f + droop), Size(34f, 20f))
    drawOval(leaf, Offset(33f, 14f + droop), Size(40f, 23f))
    drawLine(palette.mutedLeaf, Offset(35f, 27f + droop), Offset(60f, 27f + droop), 2f, StrokeCap.Round)
}

private fun DrawScope.drawCactus(plant: Plant, palette: PlantPalette, leaf: Color) {
    drawLine(leaf, Offset(50f, 85f), Offset(50f, 28f), 20f, StrokeCap.Round)
    val leftArm = Path().apply {
        moveTo(47f, 65f)
        lineTo(30f, 65f)
        quadraticTo(26f, 65f, 26f, 58f)
        lineTo(26f, 45f)
    }
    drawPath(leftArm, leaf, style = Stroke(11f, cap = StrokeCap.Round))
    val rightArm = Path().apply {
        moveTo(54f, 53f)
        lineTo(73f, 53f)
        quadraticTo(77f, 53f, 77f, 46f)
        lineTo(77f, 35f)
    }
    drawPath(rightArm, leaf, style = Stroke(10f, cap = StrokeCap.Round))
    drawLine(palette.mutedLeaf, Offset(50f, 80f), Offset(50f, 34f), 2f, StrokeCap.Round)
    if (plant.stage == PlantStage.BLOOM) {
        drawCircle(palette.bloom, 6f, Offset(50f, 19f))
        drawCircle(palette.bloomCenter, 2f, Offset(50f, 19f))
    }
}

private fun DrawScope.drawFern(palette: PlantPalette, leaf: Color, droop: Float) {
    drawLine(palette.stem, Offset(50f, 87f), Offset(50f + droop, 22f + droop), 2.5f, StrokeCap.Round)
    for (level in 0..4) {
        val y = 72f - level * 11f
        val spread = 24f - level * 3f
        val center = Offset(50f + droop * (level / 5f), y)
        drawLeaf(center, Offset(center.x - spread, y - 13f + droop), leaf)
        drawLeaf(center, Offset(center.x + spread, y - 13f + droop), leaf)
    }
    drawLeaf(Offset(50f + droop, 34f), Offset(50f + droop, 17f + droop), leaf)
}

private fun DrawScope.drawLeaf(base: Offset, tip: Offset, color: Color) {
    val delta = tip - base
    val side = Offset(-delta.y * 0.2f, delta.x * 0.2f)
    val middle = base + delta * 0.5f
    val leaf = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(middle.x + side.x, middle.y + side.y, tip.x, tip.y)
        quadraticTo(middle.x - side.x, middle.y - side.y, base.x, base.y)
        close()
    }
    drawPath(leaf, color)
}
