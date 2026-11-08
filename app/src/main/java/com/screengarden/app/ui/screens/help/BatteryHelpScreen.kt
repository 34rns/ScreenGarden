// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.domain.help.BatteryGuide

/** A local guide with explicit version caveats, not an automatic battery exemption. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatteryHelpScreen(
    manufacturer: String,
    onBack: () -> Unit,
    onAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val guides = remember(manufacturer) { BatteryGuide.ordered(manufacturer) }
    var selected by rememberSaveable(manufacturer) { mutableStateOf(guides.first()) }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onBack) { Text(stringResource(R.string.battery_back)) }
            Text(
                stringResource(R.string.battery_title),
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge.copy(textDirection = TextDirection.Content)
            )
            HelpText(R.string.battery_intro)
            HelpText(R.string.battery_caveat)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                guides.forEach { guide ->
                    FilterChip(
                        selected == guide,
                        { selected = guide },
                        { Text(stringResource(brandLabel(guide))) },
                        Modifier.heightIn(min = 48.dp)
                    )
                }
            }
            Text(
                stringResource(brandLabel(selected)),
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content)
            )
            HelpText(stepsLabel(selected))
            Button(onAppSettings, Modifier.fillMaxWidth()) { Text(stringResource(R.string.battery_app_settings)) }
            HelpText(R.string.battery_tradeoff)
            HelpText(R.string.battery_limits)
        }
    }
}

@Composable
private fun HelpText(label: Int) {
    Text(stringResource(label), style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content))
}

private fun brandLabel(guide: BatteryGuide): Int = when (guide) {
    BatteryGuide.SAMSUNG -> R.string.battery_samsung
    BatteryGuide.XIAOMI -> R.string.battery_xiaomi
    BatteryGuide.OPPO -> R.string.battery_oppo
    BatteryGuide.VIVO -> R.string.battery_vivo
    BatteryGuide.HUAWEI -> R.string.battery_huawei
    BatteryGuide.ONEPLUS -> R.string.battery_oneplus
    BatteryGuide.STOCK -> R.string.battery_stock
}

private fun stepsLabel(guide: BatteryGuide): Int = when (guide) {
    BatteryGuide.SAMSUNG -> R.string.battery_steps_samsung
    BatteryGuide.XIAOMI -> R.string.battery_steps_xiaomi
    BatteryGuide.OPPO -> R.string.battery_steps_oppo
    BatteryGuide.VIVO -> R.string.battery_steps_vivo
    BatteryGuide.HUAWEI -> R.string.battery_steps_huawei
    BatteryGuide.ONEPLUS -> R.string.battery_steps_oneplus
    BatteryGuide.STOCK -> R.string.battery_steps_stock
}
