// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.onboarding

import com.screengarden.app.domain.model.OnboardingChoices
import com.screengarden.app.domain.settings.SettingsRepository

/**
 * Writes the first-run answers into settings (P07).
 *
 * The flow's screens collect the values; this class is the only thing that persists
 * them, so the write order can be stated once. Order matters: `onboardingDone` is
 * written LAST, so a process death in the middle of the writes leaves the flow
 * visible instead of locking the user into half-applied goals.
 *
 * Only the three goal screens are mandatory. The notification answer is written the
 * moment the user makes it ([rememberNotifications]) because the permission dialog
 * can be dismissed by leaving the app entirely.
 */
class OnboardingCompletion(private val settingsRepository: SettingsRepository) {

    /** Persists the goals, then marks onboarding as done. */
    suspend fun complete(choices: OnboardingChoices) {
        settingsRepository.setDailyGoalMs(choices.dailyGoalMs)
        settingsRepository.setPickupGoal(choices.pickupGoal)
        settingsRepository.setBedtimeStart(choices.bedtimeStartText)
        settingsRepository.setBedtimeEnd(choices.bedtimeEndText)
        settingsRepository.setOnboardingDone(true)
    }

    /**
     * Records whether reminders were allowed. Denied or skipped is stored as false:
     * "off" is the documented default (16.7) and the only honest state when the user
     * said no.
     */
    suspend fun rememberNotifications(allowed: Boolean) {
        settingsRepository.setNotificationsOn(allowed)
    }
}
