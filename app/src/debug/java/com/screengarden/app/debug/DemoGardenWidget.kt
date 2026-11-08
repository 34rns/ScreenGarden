// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.debug

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.screengarden.app.R
import com.screengarden.app.domain.garden.GardenCoverage
import com.screengarden.app.domain.garden.GardenStatus
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.ui.widgets.GardenWidget

/** Only fake status enters these public captures. This receiver never exists in release. */
class DemoGardenWidget : GardenWidget() {
    override suspend fun loadStatus(context: Context): GardenStatus = GardenStatus(
        84,
        7,
        Plant("demo-widget-flower", "flower", 45, 85),
        GardenCoverage.READY
    )
}

class DemoGardenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DemoGardenWidget()
}

/** Real Glance RemoteViews hosted inside the single debug activity, not a mock screenshot. */
@Composable
fun DemoWidgetScreen() {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.widget_preview), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.widget_small_preview))
        DemoWidgetHost(128)
        Text(stringResource(R.string.widget_medium_preview))
        DemoWidgetHost(280)
    }
}

@Composable
private fun DemoWidgetHost(width: Int) {
    val context = LocalContext.current
    val host = remember { AppWidgetHost(context, 23_000 + width).apply { deleteHost() } }
    val manager = remember { AppWidgetManager.getInstance(context) }
    val id = remember {
        host.allocateAppWidgetId().also { widgetId ->
            manager.bindAppWidgetIdIfAllowed(
                widgetId,
                ComponentName(context, DemoGardenWidgetReceiver::class.java),
                Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 180)
                    if (Build.VERSION.SDK_INT >= 31) {
                        putParcelableArrayList(
                            AppWidgetManager.OPTION_APPWIDGET_SIZES,
                            arrayListOf(SizeF(width.toFloat(), 180f))
                        )
                    }
                }
            )
        }
    }
    val info = manager.getAppWidgetInfo(id)
    DisposableEffect(host, id) {
        host.startListening()
        onDispose {
            host.stopListening()
            host.deleteAppWidgetId(id)
        }
    }
    if (info != null) {
        AndroidView(
            factory = { host.createView(it, id, info).apply { setPadding(0, 0, 0, 0) } },
            modifier = Modifier.size(width.dp, 180.dp)
        )
        LaunchedEffect(id) { DemoGardenWidget().update(context, GlanceAppWidgetManager(context).getGlanceIdBy(id)) }
    } else {
        Text(stringResource(R.string.widget_preview_setup))
    }
}
