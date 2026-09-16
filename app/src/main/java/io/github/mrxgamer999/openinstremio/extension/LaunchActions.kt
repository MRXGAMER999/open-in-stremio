package io.github.mrxgamer999.openinstremio.extension

import android.content.Context
import android.content.Intent
import com.battlelancer.seriesguide.api.Action
import io.github.mrxgamer999.openinstremio.data.AndroidPackageChecker
import io.github.mrxgamer999.openinstremio.forwarder.StremioLaunchActivity
import io.github.mrxgamer999.openinstremio.forwarder.LaunchRequest
import io.github.mrxgamer999.openinstremio.forwarder.actionLabelRes
import io.github.mrxgamer999.openinstremio.forwarder.installedTargets

/**
 * Builds the actions published to SeriesGuide. Used both on [OpenInStremioExtensionReceiver]'s
 * fast path, which publishes the button while SeriesGuide is still waiting, and by the detached
 * lookup that may later upgrade it — the two must hand SeriesGuide an identical button.
 *
 * SeriesGuide shows one action per extension per title, so the label has to stand for wherever
 * the tap will actually land: the one installed player by name, or the neutral "Open in…" that
 * the forwarder answers with a chooser. It is read at publish time because the answer changes
 * when a player is installed or removed, and the button must not promise the wrong app.
 */
internal object LaunchActions {

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

    /**
     * The fallback for a title with no IMDb id. Season and episode are carried when they are
     * known: Stremio's search link has no episode form and ignores them, but Fireguy matches on
     * the name, so with them it lands on the episode rather than on the show.
     */
    fun search(
        context: Context,
        identifier: Int,
        title: String,
        season: Int? = null,
        episode: Int? = null,
    ): Action =
        Action.Builder(label(context, open = false), identifier)
            .viewIntent(
                forwarderIntent(context, LaunchRequest.TYPE_SEARCH, title).apply {
                    if (season != null && episode != null) {
                        putExtra(StremioLaunchActivity.EXTRA_SEASON, season)
                        putExtra(StremioLaunchActivity.EXTRA_EPISODE, episode)
                    }
                }
            )
            .build()

    private fun open(
        context: Context,
        identifier: Int,
        type: String,
        imdbId: String,
        title: String,
        extras: Intent.() -> Unit = {},
    ): Action =
        Action.Builder(label(context, open = true), identifier)
            .viewIntent(
                forwarderIntent(context, type, title)
                    .putExtra(StremioLaunchActivity.EXTRA_IMDB_ID, imdbId)
                    .apply(extras)
            )
            .build()

    private fun label(context: Context, open: Boolean): String =
        context.getString(actionLabelRes(AndroidPackageChecker(context).installedTargets(), open))

    private fun forwarderIntent(context: Context, type: String, title: String): Intent =
        Intent(context, StremioLaunchActivity::class.java)
            .putExtra(StremioLaunchActivity.EXTRA_TYPE, type)
            .putExtra(StremioLaunchActivity.EXTRA_TITLE, title)
}
