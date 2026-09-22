package io.github.mrxgamer999.openinstremio.extension

import android.content.Context
import android.content.Intent
import com.battlelancer.seriesguide.api.Action
import io.github.mrxgamer999.openinstremio.data.AndroidPackageChecker
import io.github.mrxgamer999.openinstremio.forwarder.StremioLaunchActivity
import io.github.mrxgamer999.openinstremio.forwarder.LaunchRequest
import io.github.mrxgamer999.openinstremio.forwarder.Target
import io.github.mrxgamer999.openinstremio.forwarder.actionLabelRes
import io.github.mrxgamer999.openinstremio.forwarder.installedTargets

/**
 * Builds the actions published to SeriesGuide. Used both on [OpenInStremioExtensionReceiver]'s
 * fast path, which publishes the button while SeriesGuide is still waiting, and by the detached
 * lookup that may later upgrade it — the two must hand SeriesGuide an identical button.
 *
 * SeriesGuide shows one action per extension per title, so the label has to stand for wherever
 * the tap will actually land: the one chosen and installed player by name, or the neutral
 * "Open in…" that the forwarder answers with a chooser. It is worked out at publish time because
 * the answer changes when a player is installed or removed, or the user changes their choice, and
 * the button must not promise the wrong app. `chosen` is the user's choice as the receiver read it.
 */
internal object LaunchActions {

    fun openMovie(
        context: Context,
        chosen: List<Target>,
        identifier: Int,
        imdbId: String,
        title: String,
        year: Int? = null,
    ): Action = open(context, chosen, identifier, LaunchRequest.TYPE_MOVIE, imdbId, title, year)

    fun openEpisode(
        context: Context,
        chosen: List<Target>,
        identifier: Int,
        imdbId: String,
        title: String,
        season: Int,
        episode: Int,
        year: Int? = null,
    ): Action =
        open(context, chosen, identifier, LaunchRequest.TYPE_SERIES, imdbId, title, year) {
            putExtra(StremioLaunchActivity.EXTRA_SEASON, season)
            putExtra(StremioLaunchActivity.EXTRA_EPISODE, episode)
        }

    /**
     * The fallback for a title with no IMDb id. Season and episode are carried when they are
     * known: Stremio's search link has no episode form and ignores them, but Fireguy matches on
     * the name, so with them it lands on the episode rather than on the show. The year is carried
     * for Fireguy's sake for the same reason — it parts same-named works there too.
     */
    fun search(
        context: Context,
        chosen: List<Target>,
        identifier: Int,
        title: String,
        season: Int? = null,
        episode: Int? = null,
        year: Int? = null,
    ): Action =
        Action.Builder(label(context, chosen, open = false), identifier)
            .viewIntent(
                forwarderIntent(context, LaunchRequest.TYPE_SEARCH, title).apply {
                    if (season != null && episode != null) {
                        putExtra(StremioLaunchActivity.EXTRA_SEASON, season)
                        putExtra(StremioLaunchActivity.EXTRA_EPISODE, episode)
                    }
                    year?.let { putExtra(StremioLaunchActivity.EXTRA_YEAR, it) }
                }
            )
            .build()

    private fun open(
        context: Context,
        chosen: List<Target>,
        identifier: Int,
        type: String,
        imdbId: String,
        title: String,
        year: Int? = null,
        extras: Intent.() -> Unit = {},
    ): Action =
        Action.Builder(label(context, chosen, open = true), identifier)
            .viewIntent(
                forwarderIntent(context, type, title)
                    .putExtra(StremioLaunchActivity.EXTRA_IMDB_ID, imdbId)
                    .apply {
                        year?.let { putExtra(StremioLaunchActivity.EXTRA_YEAR, it) }
                    }
                    .apply(extras)
            )
            .build()

    private fun label(context: Context, chosen: List<Target>, open: Boolean): String =
        context.getString(
            actionLabelRes(chosen, AndroidPackageChecker(context).installedTargets(), open)
        )

    private fun forwarderIntent(context: Context, type: String, title: String): Intent =
        Intent(context, StremioLaunchActivity::class.java)
            .putExtra(StremioLaunchActivity.EXTRA_TYPE, type)
            .putExtra(StremioLaunchActivity.EXTRA_TITLE, title)
}
