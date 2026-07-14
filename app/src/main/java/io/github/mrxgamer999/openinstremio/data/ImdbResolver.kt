package io.github.mrxgamer999.openinstremio.data

import io.github.mrxgamer999.openinstremio.data.tmdb.TmdbService
import retrofit2.HttpException

/**
 * Resolves a TMDb id to an IMDb id via the local cache first and the TMDb API second.
 * Returns null when the id cannot be resolved (no key, offline, TMDb has no mapping) —
 * callers then fall back to a Stremio search link instead of a direct detail link.
 */
interface ImdbResolver {
    suspend fun resolveMovie(tmdbId: Int?): String?

    suspend fun resolveShow(showTmdbId: Int?): String?

    /** Positive-cache lookup only — never touches the network. */
    suspend fun cachedMovie(tmdbId: Int?): String?

    /** Positive-cache lookup only — never touches the network. */
    suspend fun cachedShow(showTmdbId: Int?): String?
}

class DefaultImdbResolver(
    private val tmdb: TmdbService,
    private val cache: ImdbIdCache,
    private val apiKey: String,
    private val now: () -> Long = { System.currentTimeMillis() },
) : ImdbResolver {

    override suspend fun resolveMovie(tmdbId: Int?): String? =
        resolve(ImdbIdCache.Kind.MOVIE, tmdbId) { tmdb.movieExternalIds(it, apiKey).imdbId }

    override suspend fun resolveShow(showTmdbId: Int?): String? =
        resolve(ImdbIdCache.Kind.SHOW, showTmdbId) { tmdb.tvExternalIds(it, apiKey).imdbId }

    override suspend fun cachedMovie(tmdbId: Int?): String? =
        cached(ImdbIdCache.Kind.MOVIE, tmdbId)

    override suspend fun cachedShow(showTmdbId: Int?): String? =
        cached(ImdbIdCache.Kind.SHOW, showTmdbId)

    private suspend fun cached(kind: ImdbIdCache.Kind, tmdbId: Int?): String? =
        if (tmdbId == null || tmdbId <= 0) null else cache.get(kind, tmdbId)

    private suspend fun resolve(
        kind: ImdbIdCache.Kind,
        tmdbId: Int?,
        fetch: suspend (Int) -> String?,
    ): String? {
        if (tmdbId == null || tmdbId <= 0) return null
        cache.get(kind, tmdbId)?.let { return it }
        if (apiKey.isBlank()) return null
        // A fresh negative entry means TMDb authoritatively had no IMDb id recently;
        // skip the network until the entry expires (new titles gain ids over time).
        cache.getNegativeSince(kind, tmdbId)?.let { since ->
            if (now() - since < NEGATIVE_TTL_MS) return null
        }
        val imdbId =
            try {
                val fetched = fetch(tmdbId)?.takeUnless { it.isBlank() }
                if (fetched == null) cache.putNegative(kind, tmdbId, now())
                fetched
            } catch (e: HttpException) {
                // 404 is authoritative (no such entity); other HTTP errors may be transient.
                if (e.code() == 404) cache.putNegative(kind, tmdbId, now())
                null
            } catch (e: Exception) {
                null
            }
        if (imdbId != null) {
            cache.put(kind, tmdbId, imdbId)
            cache.clearNegative(kind, tmdbId)
        }
        return imdbId
    }

    companion object {
        const val NEGATIVE_TTL_MS: Long = 7L * 24 * 60 * 60 * 1000
    }
}
