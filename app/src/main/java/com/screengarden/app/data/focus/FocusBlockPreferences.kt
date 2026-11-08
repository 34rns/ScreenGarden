// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.focus

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.screengarden.app.data.settings.SettingsKeys
import com.screengarden.app.domain.focus.FocusBlockPolicy
import com.screengarden.app.domain.focus.FocusBlockStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/** Shares the existing DataStore instance; Android grants are never saved or restored here. */
class FocusBlockPreferences(private val store: DataStore<Preferences>) : FocusBlockStore {
    override fun observePolicy(): Flow<FocusBlockPolicy> = store.data.map {
        it[KEY]?.let(FocusBlockCodec::decode) ?: FocusBlockPolicy()
    }

    override suspend fun save(policy: FocusBlockPolicy) {
        store.edit {
            it[KEY] = FocusBlockCodec.encode(policy)
            it[SettingsKeys.GENTLE_MODE] = !policy.enabled
        }
    }

    companion object {
        const val KEY_NAME: String = "focus_blocking"
        val KEY: Preferences.Key<String> = stringPreferencesKey(KEY_NAME)
    }
}

/** Bounded, strict user configuration; never an event log or a screen-content format. */
object FocusBlockCodec {
    fun encode(policy: FocusBlockPolicy): String = JSONObject().put("enabled", policy.enabled)
        .put(
            "blocked",
            JSONArray(policy.blocked.sorted())
        ).put("allowed", JSONArray(policy.allowed.sorted())).toString()
        .also { require(it.length <= 32_768) }

    fun decode(text: String): FocusBlockPolicy {
        require(text.length in 1..32_768)
        val parser = JSONTokener(text)
        val root = parser.nextValue() as? JSONObject ?: error("Policy must be an object")
        require(parser.nextClean() == '\u0000')
        require(root.keys().asSequence().toSet() == setOf("enabled", "blocked", "allowed"))
        require(root.get("enabled") is Boolean)
        fun packages(key: String): Set<String> {
            val array = root.getJSONArray(key)
            require(array.length() <= 256)
            val values = (0 until array.length()).map {
                array.get(it).also { value -> require(value is String) } as String
            }
            require(values.distinct().size == values.size)
            return values.toSet()
        }
        return FocusBlockPolicy(root.getBoolean("enabled"), packages("blocked"), packages("allowed"))
    }
}
