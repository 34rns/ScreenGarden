// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.garden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.garden.Plant
import com.screengarden.app.domain.garden.PlantCondition
import com.screengarden.app.domain.model.ThemePreference
import com.screengarden.app.ui.theme.ScreenGardenTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Garden presentation only: data, permission requests and navigation remain with the caller. */
@Composable
fun GardenScreen(
    state: GardenUiState,
    onStartFocus: () -> Unit,
    onGrantUsage: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPlantId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPlant = state.plants.firstOrNull { it.id == selectedPlantId }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.garden_title),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Content)
                    )
                    GardenBody(stringResource(R.string.garden_subtitle))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GardenScene(
                        plants = state.plants,
                        onPlantSelected = { selectedPlantId = it.id },
                        streakDays = state.streakDays
                    )
                    if (state.plants.isNotEmpty()) GardenBody(stringResource(R.string.garden_plant_hint))
                }
                GardenSummary(score = state.score, streakDays = state.streakDays)
                GardenStatus(state = state, onGrantUsage = onGrantUsage, onRetry = onRetry)
                Button(onClick = onStartFocus, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.garden_start_focus))
                }
            }
        }
    }
    if (selectedPlant != null) PlantDetails(plant = selectedPlant, onDismiss = { selectedPlantId = null })
}

@Composable
private fun GardenSummary(score: Int?, streakDays: Int) {
    val scoreDescription = if (score == null) {
        stringResource(R.string.garden_score_unavailable)
    } else {
        stringResource(R.string.garden_score_description, score)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(88.dp).clearAndSetSemantics { contentDescription = scoreDescription },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (score == null) {
                        stringResource(R.string.garden_score_unknown)
                    } else {
                        stringResource(R.string.garden_score_number, score)
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(textDirection = TextDirection.Content)
                )
                if (score != null) {
                    Text(
                        text = stringResource(R.string.garden_score_scale),
                        style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr)
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.garden_today_score),
                style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content)
            )
            GardenBody(
                text = if (streakDays > 0) {
                    pluralStringResource(R.plurals.garden_streak_days, streakDays, streakDays)
                } else {
                    stringResource(R.string.garden_streak_empty)
                }
            )
            if (score == null) GardenBody(stringResource(R.string.garden_score_unavailable))
        }
    }
}

@Composable
private fun GardenStatus(state: GardenUiState, onGrantUsage: () -> Unit, onRetry: () -> Unit) {
    if (state.loading) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().testTag("garden_loading"))
            GardenBody(stringResource(R.string.garden_loading))
        }
        return
    }
    val title: Int
    val body: Int
    when {
        state.error -> {
            title = R.string.garden_error_title
            body = R.string.garden_error_body
        }
        state.availability == GardenAvailability.NO_PERMISSION -> {
            title = R.string.garden_permission_title
            body = R.string.garden_permission_body
        }
        state.availability == GardenAvailability.INCOMPLETE_USAGE -> {
            title = R.string.garden_incomplete_title
            body = R.string.garden_incomplete_body
        }
        state.availability == GardenAvailability.NO_DATA -> {
            title = R.string.garden_empty_title
            body = R.string.garden_empty_body
        }
        state.plants.isEmpty() -> {
            title = R.string.garden_empty_title
            body = R.string.garden_empty_ready_body
        }
        else -> return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content)
        )
        GardenBody(stringResource(body))
        when {
            state.error -> OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.garden_retry)) }
            state.availability == GardenAvailability.NO_PERMISSION -> {
                OutlinedButton(onClick = onGrantUsage) { Text(stringResource(R.string.garden_grant_usage)) }
            }
            else -> TextButton(onClick = onRetry) { Text(stringResource(R.string.garden_refresh)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantDetails(plant: Plant, onDismiss: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val plantedDate = if (plant.plantedAtUtcMillis <= 0L) {
        stringResource(R.string.garden_date_unknown)
    } else {
        // Format an instant for presentation; never calculate a day key or boundary here.
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            .format(Instant.ofEpochMilli(plant.plantedAtUtcMillis).atZone(ZoneId.systemDefault()))
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .testTag("garden_plant_details"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.garden_detail_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content)
            )
            GardenPlantArt(plant, Modifier.fillMaxWidth().height(144.dp))
            Text(
                text = stringResource(plantNameResource(plant.species)),
                style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Content)
            )
            DetailValue(stringResource(R.string.garden_detail_stage), stringResource(plantStageResource(plant.stage)))
            DetailValue(
                stringResource(R.string.garden_detail_condition),
                stringResource(plantConditionResource(plant.condition))
            )
            DetailValue(
                stringResource(R.string.garden_detail_health),
                stringResource(R.string.garden_detail_health_value, plant.health)
            )
            DetailValue(stringResource(R.string.garden_detail_planted), plantedDate)
            GardenBody(stringResource(R.string.garden_detail_reassurance))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.garden_detail_close))
            }
        }
    }
}

@Composable
private fun DetailValue(label: String, value: String) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) { }) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Content)
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content))
    }
}

@Composable
private fun GardenBody(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
    )
}

private val previewPlants = listOf(
    Plant("preview-flower", "flower", growthPoints = 45, health = 90, plantedAtUtcMillis = 1793318400000L),
    Plant("preview-bonsai", "bonsai", growthPoints = 27, health = 70, plantedAtUtcMillis = 1793491200000L),
    Plant("preview-cactus", "cactus", growthPoints = 45, health = 82, plantedAtUtcMillis = 1793664000000L),
    Plant("preview-fern", "fern", growthPoints = 15, health = 44, condition = PlantCondition.RECOVERING),
    Plant("preview-seed", "fern")
)

@Preview(showBackground = true, name = "Garden light")
@Composable
private fun GardenLightPreview() {
    ScreenGardenTheme(themePreference = ThemePreference.LIGHT) {
        GardenScreen(GardenUiState(false, GardenAvailability.READY, previewPlants, 84, 4), { }, { }, { })
    }
}

@Preview(showBackground = true, name = "Garden dark")
@Composable
private fun GardenDarkPreview() {
    ScreenGardenTheme(themePreference = ThemePreference.DARK) {
        GardenScreen(GardenUiState(false, GardenAvailability.READY, previewPlants, 84, 4), { }, { }, { })
    }
}

@Preview(showBackground = true, name = "Garden AMOLED")
@Composable
private fun GardenAmoledPreview() {
    ScreenGardenTheme(themePreference = ThemePreference.AMOLED) {
        GardenScreen(GardenUiState(false, GardenAvailability.READY, previewPlants, 84, 4), { }, { }, { })
    }
}

@Preview(showBackground = true, name = "Garden without usage access", fontScale = 1.3f)
@Composable
private fun GardenPermissionPreview() {
    ScreenGardenTheme {
        GardenScreen(GardenUiState(false, GardenAvailability.NO_PERMISSION), { }, { }, { })
    }
}

@Preview(showBackground = true, name = "Garden loading")
@Composable
private fun GardenLoadingPreview() {
    ScreenGardenTheme { GardenScreen(GardenUiState(), { }, { }, { }) }
}

@Preview(showBackground = true, name = "Garden incomplete usage")
@Composable
private fun GardenIncompletePreview() {
    ScreenGardenTheme {
        GardenScreen(GardenUiState(false, GardenAvailability.INCOMPLETE_USAGE, previewPlants), { }, { }, { })
    }
}

@Preview(showBackground = true, name = "Garden empty")
@Composable
private fun GardenEmptyPreview() {
    ScreenGardenTheme { GardenScreen(GardenUiState(loading = false), { }, { }, { }) }
}
