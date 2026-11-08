// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.world

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.screengarden.app.R
import com.screengarden.app.WorldWindow
import com.screengarden.app.domain.world.RenderMode
import com.screengarden.app.domain.world.WorldState
import com.screengarden.app.ui.theme.GardenNightColors
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

/** Fullscreen ambient view. Closing this view never stops or rewards a focus session. */
@Composable
fun WorldAmbientScreen(
    state: WorldState,
    mode: RenderMode,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    timer: String? = null,
    showClock: Boolean = true,
    onStopFocus: (() -> Unit)? = null
) {
    val owner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    var shift by remember { mutableIntStateOf(0) }
    var clock by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(owner, mode) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(30_000)
                shift = (shift + 1) % 9
                clock = LocalTime.now()
            }
        }
    }
    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
        WorldWindow(mode)
        MaterialTheme(colorScheme = if (mode == RenderMode.DESK) GardenNightColors else MaterialTheme.colorScheme) {
            Box(
                modifier.fillMaxSize().background(
                    if (mode == RenderMode.DESK) {
                        com.screengarden.app.ui.theme.DeskSky
                    } else {
                        MaterialTheme.colorScheme.background
                    }
                ).testTag("world_ambient")
            ) {
                val dx = if (mode == RenderMode.DESK) (shift % 3 - 1) * 6 else 0
                val dy = if (mode == RenderMode.DESK) (shift / 3 - 1) * 6 else 0
                Box(Modifier.fillMaxSize().offset(dx.dp, dy.dp)) {
                    WorldScene(state, Modifier.fillMaxSize(), mode)
                    Column(
                        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onExit) { Text(stringResource(R.string.world_exit)) }
                        }
                        Column(
                            Modifier.align(Alignment.CenterHorizontally).padding(bottom = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (timer != null) {
                                Text(
                                    timer,
                                    Modifier.testTag("ambient_timer"),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        textDirection = TextDirection.Ltr
                                    )
                                )
                            } else if (showClock) {
                                Text(
                                    clock.format(
                                        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                                            .withLocale(configuration.locales[0])
                                    ),
                                    modifier = Modifier.testTag("ambient_clock"),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        textDirection = TextDirection.Content
                                    )
                                )
                            }
                            if (onStopFocus != null) {
                                TextButton(onStopFocus, Modifier.testTag("ambient_stop")) {
                                    Text(stringResource(R.string.focus_stop))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
