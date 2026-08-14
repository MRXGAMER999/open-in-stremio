package io.github.mrxgamer999.openinstremio.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Records whether the extension is currently enabled inside SeriesGuide, written when the
 * extension receiver handles a subscribe or unsubscribe and read by the Home status card.
 */
class ExtensionStateStore(private val dataStore: DataStore<Preferences>) {

    val active: Flow<Boolean> = dataStore.data.map { it[KEY_ACTIVE] ?: false }

    /**
     * The extension re-asserts the flag on every request SeriesGuide makes, so an unchanged value
     * short-circuits before `edit` writes (and fsyncs) the file.
     */
    suspend fun setActive(active: Boolean) {
        if (dataStore.data.first()[KEY_ACTIVE] == active) return
        dataStore.edit { it[KEY_ACTIVE] = active }
    }

    private companion object {
        val KEY_ACTIVE = booleanPreferencesKey("extension_active")
    }
}
