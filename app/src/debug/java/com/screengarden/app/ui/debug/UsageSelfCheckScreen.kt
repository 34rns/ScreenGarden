// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.debug

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.screengarden.app.R
import com.screengarden.app.core.time.LocalDay
import com.screengarden.app.data.usage.DayUsage
import com.screengarden.app.data.usage.DayUsageResult
import com.screengarden.app.data.usage.UnlockCount
import com.screengarden.app.data.usage.UsageAccess
import com.screengarden.app.data.usage.UsageAccessCheck
import com.screengarden.app.data.usage.UsageRepository
import com.screengarden.app.data.usage.UsageSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Debug-only self-check for the P05 usage code (P05b).
 *
 * It shows what the real repository and the real system adapter return, without
 * transformation, so a number on this screen can be compared with `dumpsys
 * usagestats` and with the time the device was actually used. It exists because the
 * Android adapter could not be verified by unit tests (K35): this is the harness
 * that turns the claim "it compiles" into evidence.
 *
 * Only reachable from a debug build launched with
 * `adb shell am start -n com.screengarden.app.debug/com.screengarden.app.MainActivity
 * --es screen usage_self_check` (see DebugCatalog).
 */
@Composable
fun UsageSelfCheckScreen(usageRepository: UsageRepository, source: UsageSource, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refreshToken by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<SelfCheckState>(SelfCheckState.Loading) }

    LaunchedEffect(refreshToken) {
        state = SelfCheckState.Loading
        state = loadSelfCheck(context, usageRepository, source)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.usage_self_check_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.usage_self_check_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        when (val current = state) {
            SelfCheckState.Loading -> Line(stringResource(R.string.usage_self_check_loading), "")
            is SelfCheckState.Loaded -> SelfCheckLines(current)
        }

        Button(
            onClick = { refreshToken++ },
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text(text = stringResource(R.string.usage_self_check_refresh))
        }
    }
}

@Composable
private fun SelfCheckLines(state: SelfCheckState.Loaded) {
    Line(
        label = stringResource(R.string.usage_self_check_day),
        value = state.dayStartLabel
    )
    Line(
        label = stringResource(R.string.usage_self_check_granted),
        value = if (state.check.granted) {
            stringResource(R.string.usage_self_check_yes)
        } else {
            stringResource(R.string.usage_self_check_no)
        }
    )
    Line(
        label = stringResource(R.string.usage_self_check_method),
        value = "${state.check.method.name} (app-op ${state.check.appOpMode})"
    )
    Line(
        label = stringResource(R.string.usage_self_check_api),
        value = "${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})"
    )
    Line(
        label = stringResource(R.string.usage_self_check_launchable),
        value = state.launchablePackageCount.toString()
    )

    if (state.error != null) {
        Line(label = stringResource(R.string.usage_self_check_error), value = state.error)
        return
    }
    if (state.missingPermission || state.usage == null) {
        Line(label = stringResource(R.string.usage_self_check_screen_time), value = "")
        Text(
            text = stringResource(R.string.usage_self_check_missing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp)
        )
        return
    }

    val usage = state.usage ?: return
    Line(
        label = stringResource(R.string.usage_self_check_screen_time),
        value = formatDuration(usage.totalScreenTimeMillis)
    )
    Line(
        label = stringResource(R.string.usage_self_check_pickups),
        value = usage.pickups.toString()
    )
    Line(
        label = stringResource(R.string.usage_self_check_unlocks),
        value = when (val unlocks = usage.unlocks) {
            is UnlockCount.Supported -> unlocks.count.toString()
            UnlockCount.Unsupported -> stringResource(R.string.usage_self_check_unlocks_unsupported)
        }
    )
    Line(
        label = stringResource(R.string.usage_self_check_total_apps),
        value = usage.apps.size.toString()
    )
    Line(
        label = stringResource(R.string.usage_self_check_top_apps),
        value = ""
    )
    val topApps = usage.topApps(TOP_APPS)
    if (topApps.isEmpty()) {
        Text(
            text = stringResource(R.string.usage_self_check_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    } else {
        topApps.forEach { app ->
            Line(
                label = app.packageName,
                value = "${formatDuration(app.usageMillis)}  launches=${app.launchCount}"
            )
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Text(
        text = if (value.isEmpty()) label else "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 6.dp)
    )
}

/**
 * Reads everything the screen shows.
 *
 * Runs off the main thread because UsageStatsManager queries touch the system and
 * can be slow on a busy device (and because a debug tool must not teach the app a
 * bad pattern).
 */
private suspend fun loadSelfCheck(
    context: Context,
    usageRepository: UsageRepository,
    source: UsageSource
): SelfCheckState {
    val check = UsageAccess.check(context)
    val zone = ZoneId.systemDefault()
    val dayStart = LocalDay.startOfDay(LocalDate.now(zone), zone)

    val launchable = runCatching {
        withContext(Dispatchers.Default) { source.launchablePackages().size }
    }.getOrDefault(-1)

    val result = runCatching { usageRepository.readDay(dayStart, zone) }
    val failure = result.exceptionOrNull()
    val dayResult = result.getOrNull()

    return SelfCheckState.Loaded(
        check = check,
        dayStartLabel = Instant.ofEpochMilli(dayStart).atZone(zone).toLocalDateTime().toString(),
        launchablePackageCount = launchable,
        usage = (dayResult as? DayUsageResult.Available)?.usage,
        missingPermission = dayResult is DayUsageResult.MissingPermission,
        error = failure?.let { it::class.java.simpleName + ": " + it.message }
    )
}

/** Human-readable duration; numbers only, so no string resource is needed. */
private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "${hours}h ${minutes}m ${seconds}s" else "${minutes}m ${seconds}s"
}

/** How many app rows the self-check lists. */
private const val TOP_APPS = 5

/** What the screen is showing right now. */
private sealed interface SelfCheckState {

    data object Loading : SelfCheckState

    data class Loaded(
        val check: UsageAccessCheck,
        val dayStartLabel: String,
        val launchablePackageCount: Int,
        val usage: DayUsage?,
        val missingPermission: Boolean,
        val error: String?
    ) : SelfCheckState
}
