package io.github.mrxgamer999.openinstremio.data

import io.github.mrxgamer999.openinstremio.data.tmdb.TmdbService

/**
 * Resolves a TMDb id to an IMDb id via the local cache first and the TMDb API second.
 * Returns null when the id cannot be resolved (no key, offline, TMDb has no mapping) —
 * callers then fall back to a Stremio search link instead of a direct detail link.
 */
interface ImdbResolver {
    suspend fun resolveMovie(tmdbId: Int?): String?

    suspend fun resolveShow(showTmdbId: Int?): String?
}

class DefaultImdbResolver(
    private val tmdb: TmdbService,
    private val cache: ImdbIdCache,
    private val apiKey: String,
) : ImdbResolver {

    override suspend fun resolveMovie(tmdbId: Int?): String? =
        resolve(ImdbIdCache.Kind.MOVIE, tmdbId) { tmdb.movieExternalIds(it, apiKey).imdbId }

    override suspend fun resolveShow(showTmdbId: Int?): String? =
        resolve(ImdbIdCache.Kind.SHOW, showTmdbId) { tmdb.tvExternalIds(it, apiKey).imdbId }

    private suspend fun resolve(
        kind: ImdbIdCache.Kind,
        tmdbId: Int?,
        fetch: suspend (Int) -> String?,
    ): String? {
        if (tmdbId == null || tmdbId <= 0) return null
        cache.get(kind, tmdbId)?.let { return it }
        if (apiKey.isBlank()) return null
        val imdbId =
            try {
                fetch(tmdbId)?.takeUnless { it.isBlank() }
            } catch (e: Exception) {
                null
            }
        imdbId?.let { cache.put(kind, tmdbId, it) }
        return imdbId
    }
}
