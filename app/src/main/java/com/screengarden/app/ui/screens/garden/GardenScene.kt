// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.garden.Plant

/** A scroll-friendly plot with native, keyboard and screen-reader accessible plant controls. */
@Composable
fun GardenScene(plants: List<Plant>, onPlantSelected: (Plant) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.tertiaryContainer
    ) {
        BoxWithConstraints {
            val columns = if (maxWidth >= 340.dp) 3 else 2
            Box {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val ground = Path().apply {
                        moveTo(0f, size.height * 0.45f)
                        cubicTo(
                            size.width * 0.3f,
                            size.height * 0.34f,
                            size.width * 0.68f,
                            size.height * 0.53f,
                            size.width,
                            size.height * 0.38f
                        )
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(ground, colors.secondaryContainer)
                    drawOval(
                        colors.onTertiaryContainer.copy(alpha = 0.05f),
                        Offset(size.width * 0.62f, size.height * 0.08f),
                        Size(size.width * 0.24f, 12.dp.toPx())
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 184.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    plants.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { plant ->
                                key(plant.id) {
                                    PlantTarget(
                                        plant = plant,
                                        onClick = { onPlantSelected(plant) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            repeat(columns - row.size) { Box(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlantTarget(plant: Plant, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val name = stringResource(plantNameResource(plant.species))
    val stage = stringResource(plantStageResource(plant.stage))
    val condition = stringResource(plantConditionResource(plant.condition))
    val description = stringResource(R.string.garden_plant_description, name, stage)
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 144.dp)
            .testTag("garden_plant_${plant.id}")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = condition
                role = Role.Button
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column {
            GardenPlantArt(plant, Modifier.fillMaxWidth().height(108.dp))
            Text(
                text = name,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).clearAndSetSemantics { },
                style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Content),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun GardenPlantArt(plant: Plant, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val palette = PlantPalette(
        leaf = colors.primary,
        mutedLeaf = colors.secondary,
        stem = colors.secondary,
        bloom = colors.tertiary,
        bloomCenter = colors.onTertiary,
        soil = colors.onSecondaryContainer
    )
    val renderer = LocalPlantRenderer.current
    Canvas(modifier = modifier) { renderer.draw(this, plant, palette) }
}
