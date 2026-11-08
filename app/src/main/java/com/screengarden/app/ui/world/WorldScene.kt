// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.world

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.world.RenderMode
import com.screengarden.app.domain.world.WorldCamera
import com.screengarden.app.domain.world.WorldState
import com.screengarden.app.ui.screens.garden.LocalPlantRenderer
import com.screengarden.app.ui.screens.garden.plantNameResource
import com.screengarden.app.ui.screens.garden.plantStageResource
import com.screengarden.app.ui.screens.garden.rememberGardenMotionEnabled
import kotlinx.coroutines.delay

private data class WorldFrame(val phase: Float = 0f, val timeMs: Long = 0, val moving: Boolean = false)

/** Read only inside Canvas: capped frame changes never recompose the screen or relayout controls. */
@Composable
private fun rememberWorldFrame(mode: RenderMode, lowPower: Boolean): State<WorldFrame> {
    val moving = rememberGardenMotionEnabled() && !lowPower
    val fps = mode.frameRate(moving, reducedMotion = false, lowPower = lowPower)
    val frame = remember { mutableStateOf(WorldFrame()) }
    LaunchedEffect(fps) {
        frame.value = WorldFrame()
        if (fps > 0) {
            while (true) {
                val now = System.nanoTime() / 1_000_000
                frame.value = WorldFrame((now % 8000) / 8000f, now, true)
                delay((1000L + fps - 1) / fps)
            }
        }
    }
    return frame
}

/** Shared interactive and ambient canvas, with accessible alternatives to every camera gesture. */
@Composable
fun WorldScene(
    state: WorldState,
    modifier: Modifier = Modifier,
    mode: RenderMode = RenderMode.INTERACTIVE,
    onPlantSelected: (Plant) -> Unit = { }
) {
    var x by rememberSaveable { mutableFloatStateOf(0f) }
    var y by rememberSaveable { mutableFloatStateOf(0f) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var listOpen by rememberSaveable { mutableStateOf(false) }
    var pixels by remember { mutableStateOf(IntSize(1, 1)) }
    val interactive = mode == RenderMode.INTERACTIVE
    val camera = if (interactive) WorldCamera(x, y, zoom) else state.camera
    val uncropped = state.copy(camera = camera)
    val visible = remember(state.plants, camera, pixels, mode) {
        visibleWorldPlants(uncropped, mode, pixels.width.toFloat(), pixels.height.toFloat())
    }
    val world = uncropped.copy(plants = visible)
    val bounds = state.bounds
    val current by rememberUpdatedState(world)
    val frames = rememberWorldFrame(mode, state.lowPowerVisuals)
    var transition by remember {
        mutableStateOf(com.screengarden.app.domain.world.WorldVisualTransition(visible, visible))
    }
    var motionStarted by remember { androidx.compose.runtime.mutableLongStateOf(0) }
    LaunchedEffect(visible) {
        val frame = frames.value
        val previous = transition.sample(frame.timeMs - motionStarted, frame.moving)
        transition = com.screengarden.app.domain.world.WorldVisualTransition(previous, visible)
        motionStarted = frame.timeMs
    }
    val colors = MaterialTheme.colorScheme
    val renderer = LocalPlantRenderer.current
    val density = LocalDensity.current
    val description = stringResource(
        when (mode) {
            RenderMode.FOCUS -> R.string.world_focus_description
            RenderMode.DESK -> R.string.desk_description
            else -> R.string.world_description
        }
    )
    fun move(next: WorldCamera) {
        x = next.x
        y = next.y
        zoom = next.zoom
    }
    if (!interactive) {
        Canvas(
            modifier.fillMaxSize().onSizeChanged {
                pixels = it
            }.semantics { contentDescription = description }.testTag("world_canvas")
        ) {
            val frame = frames.value
            drawWorld(
                world.copy(
                    plants = transition.sample(frame.timeMs - motionStarted, frame.moving),
                    phase = frame.phase,
                    animationsEnabled = frame.moving
                ),
                mode,
                colors,
                renderer
            )
        }
        return
    }
    Column(modifier) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = colors.secondaryContainer) {
            Box(
                Modifier.fillMaxWidth().then(
                    if (interactive) Modifier.height(300.dp) else Modifier.heightIn(min = 300.dp)
                )
                    .onSizeChanged { pixels = it }
                    .then(
                        if (interactive) {
                            Modifier.pointerInput(state.plants) {
                                detectTransformGestures { centroid, pan, factor, _ ->
                                    val before = current.camera
                                    val unit = minOf(size.width / 6f, size.height / 4f) * before.zoom
                                    if (unit > 0f) {
                                        val anchorX = before.x + (centroid.x - size.width / 2f) / unit
                                        val anchorY = before.y + (centroid.y - size.height * 0.58f) / unit
                                        move(
                                            before.zoomBy(factor, anchorX, anchorY, bounds)
                                                .pan(-pan.x / unit, -pan.y / unit, bounds)
                                        )
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                Canvas(
                    Modifier.matchParentSize().semantics {
                        contentDescription = description
                    }.testTag("world_canvas")
                ) {
                    val frame = frames.value
                    drawWorld(
                        world.copy(
                            plants = transition.sample(frame.timeMs - motionStarted, frame.moving),
                            phase = frame.phase,
                            animationsEnabled = frame.moving
                        ),
                        mode,
                        colors,
                        renderer
                    )
                }
                if (interactive) {
                    visible.forEach { placed ->
                        val rect = worldPlantRect(placed, world, pixels.width.toFloat(), pixels.height.toFloat())
                        val name = stringResource(plantNameResource(placed.plant.species))
                        val stage = stringResource(plantStageResource(placed.plant.stage))
                        val plantDescription = stringResource(R.string.garden_plant_description, name, stage)
                        with(density) {
                            Surface(
                                onClick = { onPlantSelected(placed.plant) },
                                modifier = Modifier.align(AbsoluteAlignment.TopLeft)
                                    .absoluteOffset((rect[0]).toDp(), rect[1].toDp())
                                    .size(rect[2].toDp().coerceAtLeast(48.dp), rect[3].toDp().coerceAtLeast(48.dp))
                                    .testTag("garden_plant_${placed.plant.id}")
                                    .semantics { contentDescription = plantDescription },
                                color = colors.surface.copy(alpha = 0f)
                            ) { }
                        }
                    }
                }
            }
        }
        if (interactive) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton({ move(camera.pan(-1.6f, 0f, bounds)) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, stringResource(R.string.world_pan_left))
                }
                IconButton({ move(camera.zoomBy(1.35f, camera.x, camera.y, bounds)) }) {
                    Icon(Icons.Default.Add, stringResource(R.string.world_zoom_in))
                }
                TextButton({ move(camera.zoomBy(1 / 1.35f, camera.x, camera.y, bounds)) }) {
                    Text(stringResource(R.string.world_zoom_out))
                }
                IconButton({ move(WorldCamera()) }) {
                    Icon(Icons.Default.Home, stringResource(R.string.world_reset))
                }
                IconButton({ move(camera.pan(1.6f, 0f, bounds)) }) {
                    Icon(Icons.Default.KeyboardArrowRight, stringResource(R.string.world_pan_right))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton({ move(camera.pan(0f, -1.1f, bounds)) }) {
                    Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.world_pan_up))
                }
                TextButton({ listOpen = true }) { Text(stringResource(R.string.world_plants)) }
                IconButton({ move(camera.pan(0f, 1.1f, bounds)) }) {
                    Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.world_pan_down))
                }
            }
        }
    }
    if (listOpen) {
        AlertDialog(
            onDismissRequest = { listOpen = false },
            title = { Text(stringResource(R.string.world_plants)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(state.plants, key = { it.plant.id }) { placed ->
                        TextButton({
                            move(WorldCamera(placed.x, placed.y, 1f))
                            listOpen = false
                            onPlantSelected(placed.plant)
                        }, Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(
                                    R.string.garden_plant_description,
                                    stringResource(plantNameResource(placed.plant.species)),
                                    stringResource(plantStageResource(placed.plant.stage))
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton({ listOpen = false }) { Text(stringResource(R.string.settings_cancel)) } }
        )
    }
}
