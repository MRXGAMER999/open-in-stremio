package io.github.mrxgamer999.openinstremio.extension

import android.content.Intent
import android.util.Log
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
 * Publish-first strategy: when the IMDb id is already known (SeriesGuide provided it, or
 * the local cache has it) a single "open" action is published immediately. Otherwise a
 * "Search in Stremio" fallback is published right away — the button must never depend on
 * the network — and a TMDb lookup then tries to upgrade it to a direct "open" action.
 * publishAction may be called multiple times per request; SeriesGuide replaces the shown
 * action each time.
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
        val season = episode.season
        val number = episode.number
        val resolver = AppGraph.imdbResolver(applicationContext)

        val knownImdbId =
            episode.showImdbId?.takeUnless { it.isBlank() }
                ?: runCatching { runBlocking { resolver.cachedShow(episode.showTmdbId) } }.getOrNull()
        if (knownImdbId != null && season != null && number != null) {
            Log.i(TAG, "episode $episodeIdentifier: publishing open (id known)")
            publishAction(
                openAction(episodeIdentifier, LaunchRequest.TYPE_SERIES, knownImdbId, title, season, number)
            )
            return
        }

        Log.i(TAG, "episode $episodeIdentifier: publishing search fallback")
        publishAction(searchAction(episodeIdentifier, title))

        // Without a season/episode number a direct link is impossible; skip the lookup.
        if (season == null || number == null) return
        val resolvedImdbId =
            runCatching { runBlocking { resolver.resolveShow(episode.showTmdbId) } }.getOrNull()
        if (resolvedImdbId != null) {
            Log.i(TAG, "episode $episodeIdentifier: upgrading to open (id resolved)")
            publishAction(
                openAction(episodeIdentifier, LaunchRequest.TYPE_SERIES, resolvedImdbId, title, season, number)
            )
        }
    }

    override fun onRequest(movieIdentifier: Int, movie: Movie) {
        setActive(true)
        val title = movie.title.orEmpty()
        val resolver = AppGraph.imdbResolver(applicationContext)

        val knownImdbId =
            movie.imdbId?.takeUnless { it.isBlank() }
                ?: runCatching { runBlocking { resolver.cachedMovie(movie.tmdbId) } }.getOrNull()
        if (knownImdbId != null) {
            Log.i(TAG, "movie $movieIdentifier: publishing open (id known)")
            publishAction(openAction(movieIdentifier, LaunchRequest.TYPE_MOVIE, knownImdbId, title))
            return
        }

        Log.i(TAG, "movie $movieIdentifier: publishing search fallback")
        publishAction(searchAction(movieIdentifier, title))

        val resolvedImdbId =
            runCatching { runBlocking { resolver.resolveMovie(movie.tmdbId) } }.getOrNull()
        if (resolvedImdbId != null) {
            Log.i(TAG, "movie $movieIdentifier: upgrading to open (id resolved)")
            publishAction(openAction(movieIdentifier, LaunchRequest.TYPE_MOVIE, resolvedImdbId, title))
        }
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

    /** Best-effort: a failed flag write must never abort publishing or crash the job. */
    private fun setActive(active: Boolean) {
        runCatching {
                runBlocking { ExtensionStateStore(AppGraph.dataStore(applicationContext)).setActive(active) }
            }
            .onFailure { Log.w(TAG, "Failed to persist extension-active flag", it) }
    }

    companion object {
        private const val TAG = "OpenInStremioExt"

        // Keys the extension's SharedPreferences store; keep stable across versions.
        const val NAME = "OpenInStremioExtension"
    }
}
