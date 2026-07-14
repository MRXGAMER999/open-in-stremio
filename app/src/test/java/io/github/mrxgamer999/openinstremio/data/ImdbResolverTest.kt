package io.github.mrxgamer999.openinstremio.data

import io.github.mrxgamer999.openinstremio.data.tmdb.ExternalIdsDto
import io.github.mrxgamer999.openinstremio.data.tmdb.TmdbService
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

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
        val negatives = mutableMapOf<String, Long>()

        override suspend fun get(kind: ImdbIdCache.Kind, tmdbId: Int) = map["${kind.key}:$tmdbId"]

        override suspend fun put(kind: ImdbIdCache.Kind, tmdbId: Int, imdbId: String) {
            map["${kind.key}:$tmdbId"] = imdbId
        }

        override suspend fun getNegativeSince(kind: ImdbIdCache.Kind, tmdbId: Int) =
            negatives["${kind.key}:$tmdbId"]

        override suspend fun putNegative(kind: ImdbIdCache.Kind, tmdbId: Int, atMillis: Long) {
            negatives["${kind.key}:$tmdbId"] = atMillis
        }

        override suspend fun clearNegative(kind: ImdbIdCache.Kind, tmdbId: Int) {
            negatives.remove("${kind.key}:$tmdbId")
        }
    }

    private var nowMs = 0L

    private fun resolver(tmdb: TmdbService, cache: ImdbIdCache, apiKey: String = "key") =
        DefaultImdbResolver(tmdb, cache, apiKey, now = { nowMs })

    @Test
    fun nullOrInvalidTmdbId_returnsNull_withoutNetwork() = runTest {
        val tmdb = FakeTmdbService()
        val resolver = resolver(tmdb, InMemoryCache())

        assertNull(resolver.resolveMovie(null))
        assertNull(resolver.resolveMovie(0))
        assertNull(resolver.resolveShow(-5))
        assertEquals(0, tmdb.movieCalls + tmdb.tvCalls)
    }

    @Test
    fun cacheHit_skipsNetwork() = runTest {
        val tmdb = FakeTmdbService()
        val cache = InMemoryCache().apply { map["movie:238"] = "tt0068646" }
        val resolver = resolver(tmdb, cache)

        assertEquals("tt0068646", resolver.resolveMovie(238))
        assertEquals(0, tmdb.movieCalls)
    }

    @Test
    fun cacheMiss_fetchesAndCaches() = runTest {
        val tmdb = FakeTmdbService(tvResult = { ExternalIdsDto("tt0108778") })
        val cache = InMemoryCache()
        val resolver = resolver(tmdb, cache)

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
        val resolver = resolver(tmdb, InMemoryCache(), apiKey = "")

        assertNull(resolver.resolveMovie(238))
        assertEquals(0, tmdb.movieCalls)
    }

    @Test
    fun networkFailure_returnsNull_andCachesNothing() = runTest {
        val tmdb = FakeTmdbService(movieResult = { throw IOException("offline") })
        val cache = InMemoryCache()
        val resolver = resolver(tmdb, cache)

        assertNull(resolver.resolveMovie(238))
        assertEquals(0, cache.map.size)
        assertEquals(0, cache.negatives.size)

        // Transient failures are never negative-cached: the next resolve retries.
        assertNull(resolver.resolveMovie(238))
        assertEquals(2, tmdb.movieCalls)
    }

    @Test
    fun blankOrNullImdbIdFromTmdb_returnsNull_andCachesNoPositive() = runTest {
        val tmdb = FakeTmdbService(movieResult = { ExternalIdsDto("") }, tvResult = { ExternalIdsDto(null) })
        val cache = InMemoryCache()
        val resolver = resolver(tmdb, cache)

        assertNull(resolver.resolveMovie(238))
        assertNull(resolver.resolveShow(1668))
        assertEquals(0, cache.map.size)
    }

    @Test
    fun authoritativeNullResult_cachesNegative_andSkipsNetworkWithinTtl() = runTest {
        val tmdb = FakeTmdbService(movieResult = { ExternalIdsDto(null) })
        val cache = InMemoryCache()
        val resolver = resolver(tmdb, cache)

        assertNull(resolver.resolveMovie(238))
        assertEquals(1, tmdb.movieCalls)
        assertEquals(nowMs, cache.negatives["movie:238"])

        // Within the TTL the negative entry short-circuits the network.
        nowMs += TimeUnit.DAYS.toMillis(1)
        assertNull(resolver.resolveMovie(238))
        assertEquals(1, tmdb.movieCalls)
    }

    @Test
    fun negativeEntry_expires_andRetriesAfterTtl() = runTest {
        val tmdb = FakeTmdbService(tvResult = { ExternalIdsDto(null) })
        val cache = InMemoryCache()
        val resolver = resolver(tmdb, cache)

        assertNull(resolver.resolveShow(1668))
        assertEquals(1, tmdb.tvCalls)

        // Past the TTL the lookup runs again; a positive result replaces the negative entry.
        nowMs += TimeUnit.DAYS.toMillis(8)
        tmdb.tvResult = { ExternalIdsDto("tt0108778") }
        assertEquals("tt0108778", resolver.resolveShow(1668))
        assertEquals(2, tmdb.tvCalls)
        assertEquals("tt0108778", cache.map["show:1668"])
        assertTrue(cache.negatives.isEmpty())
    }

    @Test
    fun http404_cachesNegative() = runTest {
        val tmdb =
            FakeTmdbService(
                movieResult = { throw HttpException(Response.error<Any>(404, "".toResponseBody())) }
            )
        val cache = InMemoryCache()
        val resolver = resolver(tmdb, cache)

        assertNull(resolver.resolveMovie(238))
        assertEquals(nowMs, cache.negatives["movie:238"])
        assertNull(resolver.resolveMovie(238))
        assertEquals(1, tmdb.movieCalls)
    }

    @Test
    fun http500_isNotNegativeCached() = runTest {
        val tmdb =
            FakeTmdbService(
                movieResult = { throw HttpException(Response.error<Any>(500, "".toResponseBody())) }
            )
        val cache = InMemoryCache()
        val resolver = resolver(tmdb, cache)

        assertNull(resolver.resolveMovie(238))
        assertEquals(0, cache.negatives.size)
        assertNull(resolver.resolveMovie(238))
        assertEquals(2, tmdb.movieCalls)
    }

    @Test
    fun cachedAccessors_returnPositiveCacheOnly_withoutNetwork() = runTest {
        val tmdb =
            FakeTmdbService(
                movieResult = { ExternalIdsDto("tt0068646") },
                tvResult = { ExternalIdsDto("tt0108778") },
            )
        val cache = InMemoryCache().apply { map["movie:238"] = "tt0068646" }
        val resolver = resolver(tmdb, cache)

        assertEquals("tt0068646", resolver.cachedMovie(238))
        // A miss stays a miss — cache-only accessors never fall through to the network.
        assertNull(resolver.cachedShow(1668))
        assertNull(resolver.cachedMovie(null))
        assertNull(resolver.cachedShow(0))
        assertEquals(0, tmdb.movieCalls + tmdb.tvCalls)
    }
}
