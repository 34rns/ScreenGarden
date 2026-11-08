// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.screengarden.app.MainActivity
import com.screengarden.app.R
import com.screengarden.app.ScreenGardenApplication
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.domain.garden.GardenCoverage
import com.screengarden.app.domain.garden.GardenStatus
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantStage
import com.screengarden.app.domain.garden.summarizeGardenStatus
import com.screengarden.app.ui.screens.garden.PackPlantRenderer
import com.screengarden.app.ui.screens.garden.PlantPalette
import com.screengarden.app.ui.screens.garden.PlantRenderer
import com.screengarden.app.ui.screens.garden.PlantVisuals
import com.screengarden.app.ui.screens.garden.VectorPlantRenderer
import com.screengarden.app.ui.theme.LightBackground
import com.screengarden.app.ui.theme.LightOnBackground
import com.screengarden.app.ui.theme.LightOnSecondaryContainer
import com.screengarden.app.ui.theme.LightOnTertiary
import com.screengarden.app.ui.theme.LightPrimary
import com.screengarden.app.ui.theme.LightSecondary
import com.screengarden.app.ui.theme.LightTertiary
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Static, small/medium Glance surface; Android schedules approximate 30-minute refreshes. */
open class GardenWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(DpSize(120.dp, 180.dp), DpSize(260.dp, 180.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (status, bitmap) = withContext(Dispatchers.IO) {
            val status = loadStatus(context)
            val container = (context.applicationContext as ScreenGardenApplication).container
            val renderer = PackPlantRenderer(container.plantPacks().packs.associate { it.metadata.id to it.images })
            status to status.plant?.let { widgetPlantBitmap(it, renderer) }
        }
        provideContent { WidgetContent(context, status, bitmap) }
    }

    /** The debug-only receiver overrides this with fake state for public captures. */
    protected open suspend fun loadStatus(context: Context): GardenStatus {
        val container = (context.applicationContext as ScreenGardenApplication).container
        val zone = ZoneId.systemDefault()
        val today = LocalDay.startOfDay(System.currentTimeMillis(), zone)
        return summarizeGardenStatus(
            container.gardenRepository.observeSnapshot(Long.MIN_VALUE, today).first(),
            today,
            (container.settingsRepository.dailyGoalMs.first() / 60_000L).coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
            container.settingsRepository.pickupGoal.first(),
            container.settingsRepository.streakThreshold.first(),
            container.usageRepository.isUsageAccessGranted(),
            zone
        )
    }
}

class GardenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GardenWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        (context.applicationContext as ScreenGardenApplication).container.widgetUpdates.start()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        (context.applicationContext as ScreenGardenApplication).container.widgetUpdates.stop()
    }
}

@Composable
private fun WidgetContent(context: Context, status: GardenStatus, bitmap: android.graphics.Bitmap?) {
    val wide = LocalSize.current.width >= 260.dp
    Column(
        GlanceModifier.fillMaxSize().background(ColorProvider(LightBackground))
            .padding(12.dp).clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            context.getString(if (wide) R.string.app_name else R.string.tab_garden),
            style = widgetText(16, true),
            maxLines = 1
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (wide && bitmap != null) {
                Image(ImageProvider(bitmap), context.getString(R.string.widget_plant), GlanceModifier.size(72.dp))
            }
            Column {
                Text(
                    status.score?.let { if (wide) context.getString(R.string.widget_score, it) else it.toString() }
                        ?: context.getString(coverageLabel(status.coverage)),
                    modifier = GlanceModifier.semantics {
                        contentDescription =
                            status.score?.let { context.getString(R.string.widget_score_description, it) }
                                ?: context.getString(coverageLabel(status.coverage))
                    },
                    style = widgetText(if (status.score == null) 12 else 22, true),
                    maxLines = 2
                )
                Text(
                    context.resources.getQuantityString(R.plurals.widget_streak, status.streakDays, status.streakDays),
                    style = widgetText(12),
                    maxLines = 2
                )
            }
        }
        if (!wide && bitmap != null) {
            Image(ImageProvider(bitmap), context.getString(R.string.widget_plant), GlanceModifier.size(36.dp))
        }
    }
}

private fun widgetText(size: Int, strong: Boolean = false): TextStyle = TextStyle(
    color = ColorProvider(LightOnBackground),
    fontSize = size.sp,
    fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal
)

private fun coverageLabel(coverage: GardenCoverage): Int = when (coverage) {
    GardenCoverage.NO_PERMISSION -> R.string.widget_permission
    GardenCoverage.NO_DATA -> R.string.widget_no_data
    GardenCoverage.INCOMPLETE -> R.string.widget_incomplete
    GardenCoverage.READY -> R.string.widget_no_data
}

/** Same original Canvas plant as the app, rendered once per update without animations. */
internal fun widgetPlantBitmap(plant: Plant, renderer: PlantRenderer = VectorPlantRenderer): android.graphics.Bitmap {
    val image = ImageBitmap(192, 192)
    val palette = PlantPalette(
        LightPrimary,
        LightSecondary,
        LightSecondary,
        LightTertiary,
        LightOnTertiary,
        LightOnSecondaryContainer
    )
    val growth = when (plant.stage) {
        PlantStage.SEED -> 0f
        PlantStage.SPROUT -> 0.5f
        PlantStage.BUD -> 0.78f
        PlantStage.BLOOM -> 1f
    }
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(192f, 192f)) {
        renderer.draw(
            this,
            plant,
            palette,
            PlantVisuals(
                growth,
                if (plant.stage ==
                    PlantStage.BLOOM
                ) {
                    1f
                } else {
                    0f
                },
                (1f - plant.health / 60f).coerceIn(0f, 1f)
            )
        )
    }
    return image.asAndroidBitmap()
}
