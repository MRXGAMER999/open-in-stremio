package io.github.mrxgamer999.openinstremio.extension

import com.battlelancer.seriesguide.api.constants.OutgoingConstants
import io.github.mrxgamer999.openinstremio.extension.ExtensionHarness.Companion.FAST_PUBLISH_TIMEOUT_MS
import io.github.mrxgamer999.openinstremio.extension.ExtensionHarness.Companion.SILENCE_TIMEOUT_MS
import io.github.mrxgamer999.openinstremio.extension.ExtensionHarness.Companion.TOKEN
import io.github.mrxgamer999.openinstremio.forwarder.LaunchRequest
import io.github.mrxgamer999.openinstremio.forwarder.StremioLaunchActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * End-to-end proof that an action reaches a subscriber from the broadcast alone.
 *
 * This is the regression the receiver exists for: the button used to be published from a
 * JobIntentService, so JobScheduler decided when it appeared — often after SeriesGuide had already
 * dropped the request, which is why it showed up only sometimes. Every wait here has a deadline
 * far shorter than a deferred job could meet, so a return to the old behaviour fails it.
 */
/**
 * The titles asserted here are the ones SeriesGuide shows when Stremio is the only player on the
 * device - which is what a test device is. With Fireguy installed too they would read "Open in…"
 * and "Search in…", and the tap would open the chooser instead.
 */
class ExtensionPublishTest {

    private val harness = ExtensionHarness()

    @Before
    fun subscribe() {
        harness.subscribe()
    }

    @After
    fun unsubscribe() {
        harness.unsubscribe()
    }

    @Test
    fun episodeWithImdbId_publishesOpenActionStraightAway() {
        harness.requestEpisode(EPISODE_ID, harness.episode(showImdbId = "tt0944947"))

        val published = harness.awaitPublished(EPISODE_ID, FAST_PUBLISH_TIMEOUT_MS)
        assertEquals("Open in Stremio", published.action.title)
        assertEquals(OutgoingConstants.ACTION_TYPE_EPISODE, published.actionType)
        assertEquals(TOKEN, published.token)

        val viewIntent = published.action.viewIntent
        assertNotNull(viewIntent)
        assertEquals(
            LaunchRequest.TYPE_SERIES,
            viewIntent.getStringExtra(StremioLaunchActivity.EXTRA_TYPE),
        )
        assertEquals("tt0944947", viewIntent.getStringExtra(StremioLaunchActivity.EXTRA_IMDB_ID))
        assertEquals(1, viewIntent.getIntExtra(StremioLaunchActivity.EXTRA_SEASON, -1))
        assertEquals(1, viewIntent.getIntExtra(StremioLaunchActivity.EXTRA_EPISODE, -1))
    }

    @Test
    fun movieWithImdbId_publishesOpenActionStraightAway() {
        harness.requestMovie(MOVIE_ID, harness.movie(imdbId = "tt0133093", tmdbId = MOVIE_ID))

        val published = harness.awaitPublished(MOVIE_ID, FAST_PUBLISH_TIMEOUT_MS)
        assertEquals("Open in Stremio", published.action.title)
        assertEquals(OutgoingConstants.ACTION_TYPE_MOVIE, published.actionType)
        assertEquals(
            LaunchRequest.TYPE_MOVIE,
            published.action.viewIntent.getStringExtra(StremioLaunchActivity.EXTRA_TYPE),
        )
        assertEquals(
            "tt0133093",
            published.action.viewIntent.getStringExtra(StremioLaunchActivity.EXTRA_IMDB_ID),
        )
    }

    @Test
    fun unknownImdbId_publishesSearchFallbackStraightAway() {
        // showTmdbId 0 means no lookup can succeed either, so this fallback is all the user ever
        // gets for this title — and it still has to arrive immediately.
        harness.requestEpisode(
            UNKNOWN_ID,
            harness.episode(showImdbId = null, showTmdbId = 0, showTitle = "Some Very New Show"),
        )

        val published = harness.awaitPublished(UNKNOWN_ID, FAST_PUBLISH_TIMEOUT_MS)
        assertEquals("Search in Stremio", published.action.title)
        val viewIntent = published.action.viewIntent
        assertEquals(LaunchRequest.TYPE_SEARCH, viewIntent.getStringExtra(StremioLaunchActivity.EXTRA_TYPE))
        assertEquals(
            "Some Very New Show",
            viewIntent.getStringExtra(StremioLaunchActivity.EXTRA_TITLE),
        )
        // Stremio's search link has no episode form and ignores these, but Fireguy matches on the
        // name, so carrying them is what lets it still answer with the episode.
        assertEquals(1, viewIntent.getIntExtra(StremioLaunchActivity.EXTRA_SEASON, -1))
        assertEquals(1, viewIntent.getIntExtra(StremioLaunchActivity.EXTRA_EPISODE, -1))
    }

    @Test
    fun afterUnsubscribing_nothingIsPublished() {
        harness.unsubscribe()

        harness.requestEpisode(SILENT_ID, harness.episode(showImdbId = "tt0944947"))

        assertNull(harness.pollPublished(SILENT_ID, SILENCE_TIMEOUT_MS))
    }

    private companion object {
        // Distinct per test: a straggling action from one must not be mistaken for another's.
        const val EPISODE_ID = 63056
        const val MOVIE_ID = 603
        const val UNKNOWN_ID = 999001
        const val SILENT_ID = 999002
    }
}
