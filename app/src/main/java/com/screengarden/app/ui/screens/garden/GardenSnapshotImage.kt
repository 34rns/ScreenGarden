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
import com.screengarden.app.domain.world.RenderMode
import com.screengarden.app.domain.world.WorldSpace
import com.screengarden.app.domain.world.WorldState
import com.screengarden.app.domain.world.mapWorldPlants
import com.screengarden.app.ui.theme.GardenNightColors
import com.screengarden.app.ui.world.drawWorld

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
                drawWorld(
                    WorldState(
                        plants = mapWorldPlants(state.plants),
                        space = WorldSpace.FIELD,
                        environment = environment,
                        lowPowerVisuals = state.lowPowerVisuals
                    ),
                    if (framed) RenderMode.SNAPSHOT else RenderMode.WALLPAPER,
                    plantColors,
                    renderer,
                    palette
                )
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
