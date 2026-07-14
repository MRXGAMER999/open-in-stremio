package io.github.mrxgamer999.openinstremio.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * Persistent cache of TMDb id -> IMDb id mappings. The mapping is effectively immutable,
 * so positive entries are kept forever; a hit means the TMDb API is never asked twice.
 * Negative entries (TMDb authoritatively has no IMDb id) are timestamped so callers can
 * expire them — new titles gain IMDb ids over time.
 */
interface ImdbIdCache {
    suspend fun get(kind: Kind, tmdbId: Int): String?

    suspend fun put(kind: Kind, tmdbId: Int, imdbId: String)

    /** Epoch millis when the negative result was recorded, or null if none. */
    suspend fun getNegativeSince(kind: Kind, tmdbId: Int): Long?

    suspend fun putNegative(kind: Kind, tmdbId: Int, atMillis: Long)

    suspend fun clearNegative(kind: Kind, tmdbId: Int)

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

    override suspend fun getNegativeSince(kind: ImdbIdCache.Kind, tmdbId: Int): Long? =
        dataStore.data.first()[negativeKeyFor(kind, tmdbId)]

    override suspend fun putNegative(kind: ImdbIdCache.Kind, tmdbId: Int, atMillis: Long) {
        dataStore.edit { it[negativeKeyFor(kind, tmdbId)] = atMillis }
    }

    override suspend fun clearNegative(kind: ImdbIdCache.Kind, tmdbId: Int) {
        dataStore.edit { it.remove(negativeKeyFor(kind, tmdbId)) }
    }

    private fun keyFor(kind: ImdbIdCache.Kind, tmdbId: Int) =
        stringPreferencesKey("imdb_${kind.key}_$tmdbId")

    private fun negativeKeyFor(kind: ImdbIdCache.Kind, tmdbId: Int) =
        longPreferencesKey("imdb_neg_${kind.key}_$tmdbId")
}
