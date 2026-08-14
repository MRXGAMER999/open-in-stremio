package io.github.mrxgamer999.openinstremio.extension

import io.github.mrxgamer999.openinstremio.data.ImdbResolver
import io.github.mrxgamer999.openinstremio.extension.ExtensionHarness.Companion.FAST_PUBLISH_TIMEOUT_MS
import io.github.mrxgamer999.openinstremio.extension.ExtensionHarness.Companion.UPGRADE_TIMEOUT_MS
import io.github.mrxgamer999.openinstremio.forwarder.StremioLaunchActivity
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The properties that make a slow TMDb lookup harmless: it never delays the button, never delays
 * the next broadcast, never runs twice for one title, and still upgrades the button when it lands.
 *
 * Every title here uses a TMDb id no real lookup would have cached, because the receiver's fast
 * path reads the app's own DataStore cache rather than [ImdbLookups.resolverOverride] — the seam
 * only covers the detached lookup, which is what these tests are about.
 */
class ExtensionLookupTest {

    private val harness = ExtensionHarness()

    @Before
    fun subscribe() {
        harness.subscribe()
    }

    @After
    fun tearDown() {
        ImdbLookups.resolverOverride = null
        harness.unsubscribe()
    }

    @Test
    fun lookupThatNeverAnswers_stillPublishesTheSearchFallbackImmediately() {
        ImdbLookups.resolverOverride = FakeResolver(imdbId = null) { awaitCancellation() }

        harness.requestEpisode(
            HANGING_ID,
            harness.episode(showImdbId = null, showTmdbId = HANGING_TMDB_ID),
        )

        val published = harness.awaitPublished(HANGING_ID, FAST_PUBLISH_TIMEOUT_MS)
        assertEquals("Search in Stremio", published.action.title)
    }

    @Test
    fun lookupInFlight_doesNotHoldUpTheNextBroadcast() {
        ImdbLookups.resolverOverride = FakeResolver(imdbId = null) { awaitCancellation() }
        harness.requestEpisode(
            BLOCKING_ID,
            harness.episode(showImdbId = null, showTmdbId = BLOCKING_TMDB_ID),
        )
        harness.awaitPublished(BLOCKING_ID, FAST_PUBLISH_TIMEOUT_MS)

        // That lookup is still hanging. A receiver that held its PendingResult across it would
        // stall the broadcast queue and this second title would arrive late, or not at all.
        harness.requestEpisode(UNBLOCKED_ID, harness.episode(showImdbId = "tt0944947"))

        val published = harness.awaitPublished(UNBLOCKED_ID, FAST_PUBLISH_TIMEOUT_MS)
        assertEquals("Open in Stremio", published.action.title)
    }

    @Test
    fun concurrentRequestsForOneTitle_shareASingleLookup() {
        val gate = CompletableDeferred<Unit>()
        val resolver = FakeResolver(showImdbId = SHOW_IMDB_ID, movieImdbId = MOVIE_IMDB_ID) { gate.await() }
        ImdbLookups.resolverOverride = resolver
        val callersBefore = ImdbLookups.attachedCallers.get()

        // Three episodes of one show, as SeriesGuide asks about a season, plus a movie whose TMDb
        // id collides with the show's: the ids are namespaced per media type, so that is a
        // different title and must not be served by the show's lookup.
        val episodes = listOf(DEDUPE_ID_1, DEDUPE_ID_2, DEDUPE_ID_3)
        episodes.forEach { identifier ->
            harness.requestEpisode(
                identifier,
                harness.episode(showImdbId = null, showTmdbId = SHARED_TMDB_ID),
            )
        }
        harness.requestMovie(DEDUPE_MOVIE_ID, harness.movie(imdbId = null, tmdbId = SHARED_TMDB_ID))
        harness.awaitPublished(episodes + DEDUPE_MOVIE_ID, FAST_PUBLISH_TIMEOUT_MS)

        // Three of the four never reach the resolver at all — that is the point — so waiting on
        // call counts would prove nothing about them. Wait until all four have attached to a
        // lookup instead: only then can opening the gate not hand a straggler an empty map.
        awaitAttachedCallers(callersBefore + 4)
        gate.complete(Unit)

        // Safe to reuse the identifier set only because the gate held every upgrade back until
        // the fallbacks above had all been collected; this call would otherwise eat one of them.
        val upgrades = harness.awaitPublished(episodes + DEDUPE_MOVIE_ID, UPGRADE_TIMEOUT_MS)
        episodes.forEach { identifier ->
            val upgrade = requireNotNull(upgrades[identifier])
            assertEquals("Open in Stremio", upgrade.action.title)
            assertEquals(
                SHOW_IMDB_ID,
                upgrade.action.viewIntent.getStringExtra(StremioLaunchActivity.EXTRA_IMDB_ID),
            )
        }
        // The collision is the point: keyed on the id alone, this would carry the show's IMDb id.
        assertEquals(
            MOVIE_IMDB_ID,
            requireNotNull(upgrades[DEDUPE_MOVIE_ID])
                .action
                .viewIntent
                .getStringExtra(StremioLaunchActivity.EXTRA_IMDB_ID),
        )

        assertEquals("three episodes of one show are one lookup", 1, resolver.showCalls.get())
        assertEquals("a movie sharing the id is its own lookup", 1, resolver.movieCalls.get())
    }

    @Test
    fun resolvedLookup_upgradesTheButtonOnTheSubscriber() {
        ImdbLookups.resolverOverride = FakeResolver(showImdbId = SHOW_IMDB_ID) { delay(RESOLVE_DELAY_MS) }

        harness.requestEpisode(
            UPGRADE_ID,
            harness.episode(showImdbId = null, showTmdbId = UPGRADE_TMDB_ID),
        )

        val fallback = harness.awaitPublished(UPGRADE_ID, FAST_PUBLISH_TIMEOUT_MS)
        assertEquals("Search in Stremio", fallback.action.title)

        val upgrade = harness.awaitPublished(UPGRADE_ID, UPGRADE_TIMEOUT_MS)
        assertEquals("Open in Stremio", upgrade.action.title)
        val viewIntent = upgrade.action.viewIntent
        assertEquals(SHOW_IMDB_ID, viewIntent.getStringExtra(StremioLaunchActivity.EXTRA_IMDB_ID))
        assertEquals(1, viewIntent.getIntExtra(StremioLaunchActivity.EXTRA_SEASON, -1))
        assertEquals(1, viewIntent.getIntExtra(StremioLaunchActivity.EXTRA_EPISODE, -1))
    }

    private fun awaitAttachedCallers(target: Int) {
        val deadline = System.currentTimeMillis() + UPGRADE_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (ImdbLookups.attachedCallers.get() >= target) return
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Only ${ImdbLookups.attachedCallers.get()} of $target callers ever reached a lookup"
        )
    }

    /**
     * Stands in for the TMDb round trip so the test owns its timing. The cache accessors return
     * null because only the detached lookup goes through here; the receiver's own cache read does
     * not, and these titles are absent from it anyway.
     */
    private class FakeResolver(
        private val showImdbId: String? = null,
        private val movieImdbId: String? = null,
        private val awaitTurn: suspend () -> Unit,
    ) : ImdbResolver {

        constructor(imdbId: String?, awaitTurn: suspend () -> Unit) : this(imdbId, imdbId, awaitTurn)

        val showCalls = AtomicInteger()
        val movieCalls = AtomicInteger()

        override suspend fun resolveShow(showTmdbId: Int?): String? {
            showCalls.incrementAndGet()
            awaitTurn()
            return showImdbId
        }

        override suspend fun resolveMovie(tmdbId: Int?): String? {
            movieCalls.incrementAndGet()
            awaitTurn()
            return movieImdbId
        }

        override suspend fun cachedShow(showTmdbId: Int?): String? = null

        override suspend fun cachedMovie(tmdbId: Int?): String? = null
    }

    private companion object {
        const val SHOW_IMDB_ID = "tt0944947"
        const val MOVIE_IMDB_ID = "tt0133093"

        // Entity identifiers and TMDb ids are both kept distinct per test: a lookup left hanging
        // by one test keeps its dedupe entry until it times out, and would otherwise be joined.
        const val HANGING_ID = 999101
        const val HANGING_TMDB_ID = 999111
        const val BLOCKING_ID = 999201
        const val BLOCKING_TMDB_ID = 999211
        const val UNBLOCKED_ID = 999202
        const val DEDUPE_ID_1 = 999301
        const val DEDUPE_ID_2 = 999302
        const val DEDUPE_ID_3 = 999303
        const val DEDUPE_MOVIE_ID = 999304
        const val SHARED_TMDB_ID = 999311
        const val UPGRADE_ID = 999401
        const val UPGRADE_TMDB_ID = 999411

        const val RESOLVE_DELAY_MS = 100L
        const val POLL_INTERVAL_MS = 25L
    }
}
