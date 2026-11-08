// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.focus

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.screengarden.app.data.backup.BackupPreferences
import com.screengarden.app.data.settings.SettingsKeys
import com.screengarden.app.domain.focus.FocusBlockPolicy
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FocusBlockPreferencesTest {
    @get:Rule val temporary = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { File(temporary.root, "block.preferences_pb") }
    }

    @After fun finish() {
        scope.cancel()
    }

    @Test fun defaultsAreOffAndEditsPersistBothListsWithoutEvents() = runBlocking {
        val repository = FocusBlockPreferences(store)
        assertEquals(FocusBlockPolicy(), repository.observePolicy().first())
        val policy = FocusBlockPolicy(true, setOf("demo.video"), setOf("demo.notes"))
        repository.save(policy)
        assertEquals(policy, FocusBlockPreferences(store).observePolicy().first())
        assertFalse(store.data.first()[SettingsKeys.GENTLE_MODE]!!)
        repository.save(policy.copy(enabled = false))
        assertTrue(store.data.first()[SettingsKeys.GENTLE_MODE]!!)
    }

    @Test fun restoresKeepSelectedListsButNeverActivateBlocking() = runBlocking {
        val policy = FocusBlockPolicy(true, setOf("demo.video"), setOf("demo.notes"))
        val json = BackupPreferences.defaults().put(FocusBlockPreferences.KEY_NAME, FocusBlockCodec.encode(policy))
        BackupPreferences(store).apply(json)
        assertEquals(policy.copy(enabled = false), FocusBlockPreferences(store).observePolicy().first())
        assertTrue(store.data.first()[SettingsKeys.GENTLE_MODE]!!)
        val legacyReceipt = BackupPreferences.defaults().apply { remove(FocusBlockPreferences.KEY_NAME) }
        BackupPreferences(store).apply(legacyReceipt)
        assertEquals(FocusBlockPolicy(), FocusBlockPreferences(store).observePolicy().first())
    }

    @Test fun malformedConfigurationAndExtraEventFieldsAreRejected() {
        for (text in listOf(
            "{}",
            "[]",
            "{\"enabled\":\"true\",\"blocked\":[],\"allowed\":[]}",
            "{\"enabled\":false,\"blocked\":[\"demo.app\",\"demo.app\"],\"allowed\":[]}",
            FocusBlockCodec.encode(FocusBlockPolicy()) + "{}",
            JSONObject(FocusBlockCodec.encode(FocusBlockPolicy())).put("event", "content").toString()
        )) {
            assertThrows(Exception::class.java) { FocusBlockCodec.decode(text) }
        }
    }
}
