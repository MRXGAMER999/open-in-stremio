package io.github.mrxgamer999.openinstremio.extension

import android.content.Context
import android.content.Intent
import com.battlelancer.seriesguide.api.Action
import io.github.mrxgamer999.openinstremio.R
import io.github.mrxgamer999.openinstremio.forwarder.LaunchRequest
import io.github.mrxgamer999.openinstremio.forwarder.StremioLaunchActivity

/**
 * Builds the actions published to SeriesGuide. Used both on [OpenInStremioExtensionReceiver]'s
 * fast path, which publishes the button while SeriesGuide is still waiting, and by the detached
 * lookup that may later upgrade it — the two must hand SeriesGuide an identical button.
 */
internal object StremioActions {

    fun openMovie(context: Context, identifier: Int, imdbId: String, title: String): Action =
        open(context, identifier, LaunchRequest.TYPE_MOVIE, imdbId, title)

    fun openEpisode(
        context: Context,
        identifier: Int,
        imdbId: String,
        title: String,
        season: Int,
        episode: Int,
    ): Action =
        open(context, identifier, LaunchRequest.TYPE_SERIES, imdbId, title) {
            putExtra(StremioLaunchActivity.EXTRA_SEASON, season)
            putExtra(StremioLaunchActivity.EXTRA_EPISODE, episode)
        }

    fun search(context: Context, identifier: Int, title: String): Action =
        Action.Builder(context.getString(R.string.action_search_in_stremio), identifier)
            .viewIntent(forwarderIntent(context, LaunchRequest.TYPE_SEARCH, title))
            .build()

    private fun open(
        context: Context,
        identifier: Int,
        type: String,
        imdbId: String,
        title: String,
        extras: Intent.() -> Unit = {},
    ): Action =
        Action.Builder(context.getString(R.string.action_open_in_stremio), identifier)
            .viewIntent(
                forwarderIntent(context, type, title)
                    .putExtra(StremioLaunchActivity.EXTRA_IMDB_ID, imdbId)
                    .apply(extras)
            )
            .build()

    private fun forwarderIntent(context: Context, type: String, title: String): Intent =
        Intent(context, StremioLaunchActivity::class.java)
            .putExtra(StremioLaunchActivity.EXTRA_TYPE, type)
            .putExtra(StremioLaunchActivity.EXTRA_TITLE, title)
}
