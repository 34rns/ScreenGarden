// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.garden.Hemisphere

/** Local-only visual preferences; writes and failures are owned by the composition root. */
@Composable
fun EnvironmentControls(
    hemisphere: Hemisphere,
    lowPower: Boolean,
    onHemisphere: (Hemisphere) -> Unit,
    onLowPower: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    saving: Boolean = false,
    error: Boolean = false
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.environment_settings),
            Modifier.semantics {
                heading()
            },
            style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content)
        )
        Text(
            stringResource(R.string.environment_hemisphere_copy),
            style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content)
        )
        Column {
            Hemisphere.entries.forEach { choice ->
                FilterChip(
                    selected = hemisphere == choice,
                    onClick = { onHemisphere(choice) },
                    label = {
                        Text(
                            stringResource(
                                if (choice ==
                                    Hemisphere.NORTHERN
                                ) {
                                    R.string.environment_north
                                } else {
                                    R.string.environment_south
                                }
                            )
                        )
                    },
                    enabled = !saving,
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
        val label = stringResource(R.string.environment_low_power)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(label, Modifier.weight(1f))
            Switch(lowPower, onLowPower, Modifier.semantics { contentDescription = label }, enabled = !saving)
        }
        Text(
            stringResource(R.string.environment_low_power_copy),
            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
        )
        if (error) Text(stringResource(R.string.environment_save_error), color = MaterialTheme.colorScheme.error)
    }
}
