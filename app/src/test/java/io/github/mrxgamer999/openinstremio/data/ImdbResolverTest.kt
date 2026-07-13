package io.github.mrxgamer999.openinstremio.data

import io.github.mrxgamer999.openinstremio.data.tmdb.ExternalIdsDto
import io.github.mrxgamer999.openinstremio.data.tmdb.TmdbService
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImdbResolverTest {

    private class FakeTmdbService(
        var movieResult: () -> ExternalIdsDto = { ExternalIdsDto(null) },
        var tvResult: () -> ExternalIdsDto = { ExternalIdsDto(null) },
    ) : TmdbService {
        var movieCalls = 0
        var tvCalls = 0

        override suspend fun movieExternalIds(tmdbId: Int, apiKey: String): ExternalIdsDto {
            movieCalls++
            return movieResult()
        }

        override suspend fun tvExternalIds(tmdbId: Int, apiKey: String): ExternalIdsDto {
            tvCalls++
            return tvResult()
        }
    }

    private class InMemoryCache : ImdbIdCache {
        val map = mutableMapOf<String, String>()

        override suspend fun get(kind: ImdbIdCache.Kind, tmdbId: Int) = map["${kind.key}:$tmdbId"]

        override suspend fun put(kind: ImdbIdCache.Kind, tmdbId: Int, imdbId: String) {
            map["${kind.key}:$tmdbId"] = imdbId
        }
    }

    @Test
    fun nullOrInvalidTmdbId_returnsNull_withoutNetwork() = runTest {
        val tmdb = FakeTmdbService()
        val resolver = DefaultImdbResolver(tmdb, InMemoryCache(), apiKey = "key")

        assertNull(resolver.resolveMovie(null))
        assertNull(resolver.resolveMovie(0))
        assertNull(resolver.resolveShow(-5))
        assertEquals(0, tmdb.movieCalls + tmdb.tvCalls)
    }

    @Test
    fun cacheHit_skipsNetwork() = runTest {
        val tmdb = FakeTmdbService()
        val cache = InMemoryCache().apply { map["movie:238"] = "tt0068646" }
        val resolver = DefaultImdbResolver(tmdb, cache, apiKey = "key")

        assertEquals("tt0068646", resolver.resolveMovie(238))
        assertEquals(0, tmdb.movieCalls)
    }

    @Test
    fun cacheMiss_fetchesAndCaches() = runTest {
        val tmdb = FakeTmdbService(tvResult = { ExternalIdsDto("tt0108778") })
        val cache = InMemoryCache()
        val resolver = DefaultImdbResolver(tmdb, cache, apiKey = "key")

        assertEquals("tt0108778", resolver.resolveShow(1668))
        assertEquals("tt0108778", cache.map["show:1668"])
        assertEquals(1, tmdb.tvCalls)

        // Second resolve is served from the cache.
        assertEquals("tt0108778", resolver.resolveShow(1668))
        assertEquals(1, tmdb.tvCalls)
    }

    @Test
    fun blankApiKey_returnsNull_withoutNetwork() = runTest {
        val tmdb = FakeTmdbService(movieResult = { ExternalIdsDto("tt0068646") })
        val resolver = DefaultImdbResolver(tmdb, InMemoryCache(), apiKey = "")

        assertNull(resolver.resolveMovie(238))
        assertEquals(0, tmdb.movieCalls)
    }

    @Test
    fun networkFailure_returnsNull_andCachesNothing() = runTest {
        val tmdb = FakeTmdbService(movieResult = { throw IOException("offline") })
        val cache = InMemoryCache()
        val resolver = DefaultImdbResolver(tmdb, cache, apiKey = "key")

        assertNull(resolver.resolveMovie(238))
        assertEquals(0, cache.map.size)
    }

    @Test
    fun blankOrNullImdbIdFromTmdb_returnsNull_andCachesNothing() = runTest {
        val tmdb = FakeTmdbService(movieResult = { ExternalIdsDto("") }, tvResult = { ExternalIdsDto(null) })
        val cache = InMemoryCache()
        val resolver = DefaultImdbResolver(tmdb, cache, apiKey = "key")

        assertNull(resolver.resolveMovie(238))
        assertNull(resolver.resolveShow(1668))
        assertEquals(0, cache.map.size)
    }
}
