// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.ui.screens.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.screengarden.app.domain.backup.BackupPreview
import com.screengarden.app.domain.backup.RestoreMode
import com.screengarden.app.ui.theme.ScreenGardenTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupControlsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun replacementRequiresSecondConfirmationAndMergeDoesNotEraseSilently() {
        var restored: RestoreMode? = null
        compose.setContent {
            ScreenGardenTheme {
                BackupControls(
                    BackupUiState(preview = BackupPreview(1000, mapOf("plants" to 3))),
                    {},
                    {},
                    { restored = it },
                    {}
                )
            }
        }
        compose.onNodeWithText("Replace with this backup").performClick()
        assertNull(restored)
        compose.onNodeWithText("Cancel").performClick()
        assertNull(restored)
        compose.onNodeWithText("Merge with my data").performClick()
        assertEquals(RestoreMode.MERGE, restored)
    }
}
