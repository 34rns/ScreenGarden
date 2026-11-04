// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.screengarden.app.R
import com.screengarden.app.domain.model.OnboardingChoices

/**
 * The first-run flow (P07): garden idea, privacy, usage access, notifications, goals.
 *
 * It is plain Compose rather than a navigation graph: the steps are a short linear
 * sequence with one branch (the notification step only exists on Android 13+ and only
 * while the permission is missing), and nothing here should end up on the back stack.
 *
 * The screen never touches the data layer. Two things come from outside instead:
 * - [isUsageAccessGranted] / [onOpenUsageAccessSettings] wrap [UsageAccess], the one
 *   helper that knows how to check and request that app-op (D47), so the UI cannot
 *   grow a second, subtly different check;
 * - [onComplete] persists the answers. Keeping storage out of the composable is what
 *   lets the happy path be tested with a plain lambda.
 *
 * Grant detection is lifecycle aware: the user leaves for system Settings and comes
 * back, so the state is re-checked on every resume rather than only once.
 */
@Composable
fun OnboardingScreen(
    initialChoices: OnboardingChoices,
    isUsageAccessGranted: () -> Boolean,
    onOpenUsageAccessSettings: () -> Unit,
    onNotificationsChosen: (Boolean) -> Unit,
    onComplete: (OnboardingChoices) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Evaluated once: the composition of the step list must not change under the user.
    val askForNotifications = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)
    }
    val steps = remember(askForNotifications) {
        if (askForNotifications) {
            ALL_STEPS
        } else {
            ALL_STEPS.filterNot { it == OnboardingStep.Notifications }
        }
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var choices by rememberSaveable(stateSaver = OnboardingChoicesSaver) { mutableStateOf(initialChoices) }
    var usageGranted by remember { mutableStateOf(isUsageAccessGranted()) }
    val lastIndex = steps.lastIndex

    fun advance() {
        index = (index + 1).coerceAtMost(lastIndex)
    }

    LifecycleResumeEffect(Unit) {
        usageGranted = isUsageAccessGranted()
        onPauseOrDispose { }
    }

    val notificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onNotificationsChosen(granted)
        advance()
    }

    val step = steps[index.coerceIn(0, lastIndex)]
    val primary: Pair<Int, () -> Unit> = when (step) {
        OnboardingStep.Garden, OnboardingStep.Privacy -> R.string.onboarding_continue to { advance() }
        OnboardingStep.Usage -> if (usageGranted) {
            R.string.onboarding_continue to { advance() }
        } else {
            R.string.onboarding_usage_open_settings to {
                onOpenUsageAccessSettings()
                // The user is about to leave for system Settings. Checking now costs
                // nothing and covers the case where the grant already exists; the
                // resume effect covers the return.
                usageGranted = isUsageAccessGranted()
            }
        }
        OnboardingStep.Notifications -> R.string.onboarding_notifications_allow to {
            notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        OnboardingStep.Goals -> R.string.onboarding_finish to { onComplete(choices) }
    }
    val secondary: Pair<Int, () -> Unit>? = when (step) {
        OnboardingStep.Usage -> R.string.onboarding_usage_skip to { advance() }
        OnboardingStep.Notifications -> R.string.onboarding_notifications_not_now to {
            onNotificationsChosen(false)
            advance()
        }
        else -> null
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OnboardingProgress(current = index + 1, total = steps.size)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 24.dp)
            ) {
                when (step) {
                    OnboardingStep.Garden -> OnboardingPage(
                        titleRes = R.string.onboarding_garden_title,
                        bodyRes = R.string.onboarding_garden_body,
                        icon = Icons.Filled.Favorite
                    )
                    OnboardingStep.Privacy -> OnboardingPage(
                        titleRes = R.string.onboarding_privacy_title,
                        bodyRes = R.string.onboarding_privacy_body,
                        icon = Icons.Filled.Lock
                    )
                    OnboardingStep.Usage -> OnboardingPage(
                        titleRes = R.string.onboarding_usage_title,
                        bodyRes = R.string.onboarding_usage_body,
                        icon = Icons.Filled.Info
                    ) {
                        Text(
                            text = stringResource(
                                if (usageGranted) {
                                    R.string.onboarding_usage_granted
                                } else {
                                    R.string.onboarding_usage_not_granted
                                }
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (usageGranted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(top = 20.dp)
                        )
                        if (!usageGranted) {
                            Text(
                                text = stringResource(R.string.onboarding_usage_skip_explanation),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDirection = TextDirection.Content
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                    OnboardingStep.Notifications -> OnboardingPage(
                        titleRes = R.string.onboarding_notifications_title,
                        bodyRes = R.string.onboarding_notifications_body,
                        icon = Icons.Filled.Notifications
                    )
                    OnboardingStep.Goals -> Column(modifier = Modifier.fillMaxWidth()) {
                        OnboardingPage(
                            titleRes = R.string.onboarding_goals_title,
                            bodyRes = R.string.onboarding_goals_body,
                            icon = Icons.Filled.Settings
                        )
                        GoalSetup(
                            choices = choices,
                            onChoicesChange = { updated -> choices = updated },
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = primary.second,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(text = stringResource(primary.first))
                }
                if (secondary != null) {
                    TextButton(
                        onClick = secondary.second,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(text = stringResource(secondary.first))
                    }
                }
                if (index > 0) {
                    TextButton(
                        onClick = { index -= 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(text = stringResource(R.string.onboarding_back))
                    }
                }
            }
        }
    }
}

/** One calm page: an icon that carries no meaning, a heading and a short paragraph. */
@Composable
private fun OnboardingPage(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    icon: ImageVector,
    extra: @Composable ColumnScope.() -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            // Decorative: the heading right below already says what this is.
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 16.dp)
                .semantics { heading() }
        )
        Text(
            text = stringResource(bodyRes),
            // English copy in an RTL locale must still read left to right, or the
            // trailing full stop lands at the start of the sentence (P00b).
            style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
        extra()
    }
}

/**
 * The dot row. Its content description ("Step 2 of 5") is the only accessible cue for
 * progress, because coloured dots are invisible to a screen reader.
 */
@Composable
private fun OnboardingProgress(current: Int, total: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.onboarding_progress, current, total)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(total) { position ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (position < current) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            )
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Survives a rotation: the flow is short but losing three chosen goals to a turn of
 * the phone would be a bad first impression.
 */
private val OnboardingChoicesSaver: Saver<OnboardingChoices, Any> = listSaver(
    save = { choices ->
        listOf<Any>(
            choices.dailyGoalMs,
            choices.pickupGoal,
            choices.bedtimeStartMinutes,
            choices.bedtimeEndMinutes
        )
    },
    restore = { saved ->
        OnboardingChoices(
            dailyGoalMs = saved[0] as Long,
            pickupGoal = saved[1] as Int,
            bedtimeStartMinutes = saved[2] as Int,
            bedtimeEndMinutes = saved[3] as Int
        )
    }
)

private enum class OnboardingStep { Garden, Privacy, Usage, Notifications, Goals }

private val ALL_STEPS: List<OnboardingStep> = listOf(
    OnboardingStep.Garden,
    OnboardingStep.Privacy,
    OnboardingStep.Usage,
    OnboardingStep.Notifications,
    OnboardingStep.Goals
)
