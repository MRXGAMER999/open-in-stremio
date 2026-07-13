package io.github.mrxgamer999.openinstremio.extension

import android.content.Intent
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.api.Episode
import com.battlelancer.seriesguide.api.Movie
import com.battlelancer.seriesguide.api.SeriesGuideExtension
import io.github.mrxgamer999.openinstremio.R
import io.github.mrxgamer999.openinstremio.data.AppGraph
import io.github.mrxgamer999.openinstremio.data.ExtensionStateStore
import io.github.mrxgamer999.openinstremio.forwarder.LaunchRequest
import io.github.mrxgamer999.openinstremio.forwarder.StremioLaunchActivity
import kotlinx.coroutines.runBlocking

/**
 * Publishes the "Open in Stremio" action for every movie and episode SeriesGuide shows.
 *
 * IMDb-id precedence: the id SeriesGuide already provides, then the local cache, then a
 * TMDb external-ids lookup (bounded by tight OkHttp timeouts; this runs on the
 * JobIntentService worker thread, never the main thread). When no id can be resolved the
 * published action becomes "Search in Stremio" so the button never leads nowhere.
 */
class OpenInStremioExtension : SeriesGuideExtension(NAME) {

    override fun onEnabled() {
        setActive(true)
    }

    override fun onDisabled() {
        setActive(false)
    }

    override fun onRequest(episodeIdentifier: Int, episode: Episode) {
        setActive(true)
        val title = episode.showTitle?.takeUnless { it.isBlank() } ?: episode.title.orEmpty()
        publishSafely(episodeIdentifier, title) {
            val showImdbId =
                episode.showImdbId?.takeUnless { it.isBlank() }
                    ?: runBlocking { AppGraph.imdbResolver(applicationContext).resolveShow(episode.showTmdbId) }
            val season = episode.season
            val number = episode.number
            if (showImdbId != null && season != null && number != null) {
                openAction(
                    identifier = episodeIdentifier,
                    type = LaunchRequest.TYPE_SERIES,
                    imdbId = showImdbId,
                    title = title,
                    season = season,
                    episode = number,
                )
            } else {
                searchAction(episodeIdentifier, title)
            }
        }
    }

    override fun onRequest(movieIdentifier: Int, movie: Movie) {
        setActive(true)
        val title = movie.title.orEmpty()
        publishSafely(movieIdentifier, title) {
            val imdbId =
                movie.imdbId?.takeUnless { it.isBlank() }
                    ?: runBlocking { AppGraph.imdbResolver(applicationContext).resolveMovie(movie.tmdbId) }
            if (imdbId != null) {
                openAction(
                    identifier = movieIdentifier,
                    type = LaunchRequest.TYPE_MOVIE,
                    imdbId = imdbId,
                    title = title,
                )
            } else {
                searchAction(movieIdentifier, title)
            }
        }
    }

    /** Always publish something; a failed lookup must never leave a stale or missing action. */
    private fun publishSafely(identifier: Int, title: String, buildAction: () -> Action) {
        val action =
            try {
                buildAction()
            } catch (e: Exception) {
                searchAction(identifier, title)
            }
        publishAction(action)
    }

    private fun openAction(
        identifier: Int,
        type: String,
        imdbId: String,
        title: String,
        season: Int? = null,
        episode: Int? = null,
    ): Action =
        Action.Builder(getString(R.string.action_open_in_stremio), identifier)
            .viewIntent(
                forwarderIntent(type, title).putExtra(StremioLaunchActivity.EXTRA_IMDB_ID, imdbId).apply {
                    season?.let { putExtra(StremioLaunchActivity.EXTRA_SEASON, it) }
                    episode?.let { putExtra(StremioLaunchActivity.EXTRA_EPISODE, it) }
                }
            )
            .build()

    private fun searchAction(identifier: Int, title: String): Action =
        Action.Builder(getString(R.string.action_search_in_stremio), identifier)
            .viewIntent(forwarderIntent(LaunchRequest.TYPE_SEARCH, title))
            .build()

    private fun forwarderIntent(type: String, title: String): Intent =
        Intent(applicationContext, StremioLaunchActivity::class.java)
            .putExtra(StremioLaunchActivity.EXTRA_TYPE, type)
            .putExtra(StremioLaunchActivity.EXTRA_TITLE, title)

    private fun setActive(active: Boolean) {
        runBlocking { ExtensionStateStore(AppGraph.dataStore(applicationContext)).setActive(active) }
    }

    companion object {
        // Keys the extension's SharedPreferences store; keep stable across versions.
        const val NAME = "OpenInStremioExtension"
    }
}
