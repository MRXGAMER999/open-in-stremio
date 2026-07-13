package io.github.mrxgamer999.openinstremio.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * Persistent cache of TMDb id -> IMDb id mappings. The mapping is effectively immutable,
 * so entries are kept forever; a hit means the TMDb API is never asked twice.
 */
interface ImdbIdCache {
    suspend fun get(kind: Kind, tmdbId: Int): String?

    suspend fun put(kind: Kind, tmdbId: Int, imdbId: String)

    enum class Kind(val key: String) {
        MOVIE("movie"),
        SHOW("show"),
    }
}

class DataStoreImdbIdCache(private val dataStore: DataStore<Preferences>) : ImdbIdCache {

    override suspend fun get(kind: ImdbIdCache.Kind, tmdbId: Int): String? =
        dataStore.data.first()[keyFor(kind, tmdbId)]

    override suspend fun put(kind: ImdbIdCache.Kind, tmdbId: Int, imdbId: String) {
        if (imdbId.isBlank()) return
        dataStore.edit { it[keyFor(kind, tmdbId)] = imdbId }
    }

    private fun keyFor(kind: ImdbIdCache.Kind, tmdbId: Int) =
        stringPreferencesKey("imdb_${kind.key}_$tmdbId")
}
