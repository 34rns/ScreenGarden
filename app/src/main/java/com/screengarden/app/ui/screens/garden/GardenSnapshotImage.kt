// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.screengarden.app.domain.garden.GardenLight
import com.screengarden.app.domain.garden.GardenWeather
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.domain.garden.PlantStage
import com.screengarden.app.ui.theme.GardenNightColors
import com.screengarden.app.ui.theme.gardenEnvironmentColors

/** A static image of the supplied snapshot; no usage reads, motion or garden rewards. */
internal fun renderGardenSnapshot(
    state: GardenUiState,
    colors: ColorScheme,
    caption: String? = null,
    renderer: PlantRenderer = VectorPlantRenderer,
    width: Int = 1024,
    height: Int = 768,
    framed: Boolean = true
): ImageBitmap {
    require(width in 96..2048 && height in 96..2048)
    val image = ImageBitmap(width, height)
    val canvas = Canvas(image)
    val captionHeight = if (caption == null) 0f else 144f
    val environment = if (state.isBedtime) {
        state.environment.copy(light = GardenLight.NIGHT, weather = GardenWeather.CLEAR)
    } else {
        state.environment
    }
    val plantColors = if (environment.light == GardenLight.NIGHT) GardenNightColors else colors
    val sceneColors = gardenEnvironmentColors(environment, plantColors)
    val palette = PlantPalette(
        plantColors.primary,
        plantColors.secondary,
        plantColors.secondary,
        plantColors.tertiary,
        plantColors.onTertiary,
        plantColors.onSecondaryContainer
    )
    val margin = if (framed) 24f else 0f
    CanvasDrawScope().draw(Density(2f), LayoutDirection.Ltr, canvas, Size(width.toFloat(), height.toFloat())) {
        drawRect(colors.background)
        inset(margin, margin, margin, margin + captionHeight) {
            val boundary = Path().apply {
                addRoundRect(RoundRect(Rect(0f, 0f, size.width, size.height), CornerRadius(if (framed) 40f else 0f)))
            }
            clipPath(boundary) {
                drawGardenBackdrop(
                    sceneColors,
                    plantColors,
                    environment.light,
                    Brush.verticalGradient(listOf(sceneColors.sky, sceneColors.horizon))
                )
                drawGardenWeather(environment.weather, 0f, plantColors.onTertiaryContainer, state.lowPowerVisuals)
                val columns = state.plants.size.coerceIn(1, 3)
                val rows = ((state.plants.size + columns - 1) / columns).coerceAtLeast(1)
                val width = size.width / columns
                // Leave the wallpaper's lower area clear for Android preview controls and the Home dock.
                val plotFraction = if (!framed && size.height > size.width) 0.4f else 0.65f
                val height = size.height * plotFraction / rows
                state.plants.forEachIndexed { index, plant ->
                    val x = index % columns * width
                    val y = size.height * 0.3f + index / columns * height
                    val right = size.width - x - width
                    val bottom = size.height - y - height
                    inset(x, y, right, bottom) {
                        val growth = when (plant.stage) {
                            PlantStage.SEED -> 0f
                            PlantStage.SPROUT -> 0.5f
                            PlantStage.BUD -> 0.78f
                            PlantStage.BLOOM -> 1f
                        }
                        val wilt = if (plant.condition == PlantCondition.GROWING) {
                            0f
                        } else {
                            ((60 - plant.health) / 50f).coerceIn(0f, 1f)
                        }
                        renderer.draw(this, plant, palette, PlantVisuals(growth, if (growth == 1f) 1f else 0f, wilt))
                    }
                }
            }
        }
    }
    if (caption != null) {
        val paint = TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.onBackground.toArgb()
            textSize = 30f
        }
        val text = StaticLayout.Builder.obtain(caption, 0, caption.length, paint, width - 48)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(3)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.nativeCanvas.save()
        canvas.nativeCanvas.translate(24f, height - captionHeight)
        text.draw(canvas.nativeCanvas)
        canvas.nativeCanvas.restore()
    }
    return image
}
