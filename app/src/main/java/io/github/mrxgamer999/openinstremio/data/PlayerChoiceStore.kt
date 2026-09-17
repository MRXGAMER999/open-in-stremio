package io.github.mrxgamer999.openinstremio.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The user's [PlayerChoice], written by the setup guide and Settings and read by Home, the
 * extension receiver (for the button's label) and the forwarder (for where a tap goes).
 */
class PlayerChoiceStore(private val dataStore: DataStore<Preferences>) {

    /** An unset or unrecognised value reads as [PlayerChoice.BOTH]. */
    val choice: Flow<PlayerChoice> =
        dataStore.data.map { parse(it[KEY_CHOICE]) ?: PlayerChoice.BOTH }.distinctUntilChanged()

    /** False until the user has picked, which is what opens the setup guide on first launch. */
    val isChosen: Flow<Boolean> = dataStore.data.map { parse(it[KEY_CHOICE]) != null }

    suspend fun set(choice: PlayerChoice) {
        if (dataStore.data.first()[KEY_CHOICE] == choice.name) return
        dataStore.edit { it[KEY_CHOICE] = choice.name }
    }

    companion object {
        val KEY_CHOICE = stringPreferencesKey("player_choice")

        private fun parse(value: String?): PlayerChoice? =
            PlayerChoice.entries.firstOrNull { it.name == value }
    }
}
