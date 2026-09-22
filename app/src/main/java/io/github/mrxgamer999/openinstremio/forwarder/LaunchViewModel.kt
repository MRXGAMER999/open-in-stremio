package io.github.mrxgamer999.openinstremio.forwarder

import androidx.lifecycle.ViewModel
import io.github.mrxgamer999.openinstremio.data.PackageChecker
import io.github.mrxgamer999.openinstremio.deeplink.FireguyLinks
import io.github.mrxgamer999.openinstremio.deeplink.StremioLinks

/** What the action button asked the forwarder to open, parsed from the launch intent. */
data class LaunchRequest(
    val type: String?,
    val imdbId: String?,
    val season: Int,
    val episode: Int,
    val title: String?,
    val year: Int? = null,
) {
    companion object {
        const val TYPE_MOVIE = "movie"
        const val TYPE_SERIES = "series"
        const val TYPE_SEARCH = "search"
    }
}

sealed interface LaunchDecision {
    /** The target is installed: fire this deep link and finish. */
    data class Launch(val target: Target, val uri: String) : LaunchDecision

    /**
     * More than one target can serve this request, so the tap has to ask which one.
     *
     * [isSearch] carries the same distinction the button's own label was chosen by, so the rows
     * say "Search in X" under a "Search in…" button rather than promising to open a title that
     * has no id to open.
     */
    data class ShowChooser(val targets: List<Target>, val isSearch: Boolean) : LaunchDecision

    /** The chosen target is missing: show its install dialog. */
    data class ShowMissing(val target: Target, val title: String?) : LaunchDecision

    /** Nothing usable in the intent: exit quietly. */
    data object Finish : LaunchDecision
}

/**
 * Presentation logic for [StremioLaunchActivity]: builds the deep link for the request and decides
 * between launching a target, asking which one, and showing an install dialog.
 * Pure and synchronous so it is unit-testable without an Activity.
 */
class LaunchViewModel(
    private val packageChecker: PackageChecker,
    private val isTv: Boolean,
) : ViewModel() {

    /**
     * Only the [chosen] players are considered, and the chooser only appears when more than one
     * of them can open the request. With one, the tap goes straight there. With none, it lands on
     * the first chosen player's install nudge. For a user who chose both, that is Stremio's, as it
     * was before Fireguy existed.
     */
    fun decide(request: LaunchRequest, chosen: List<Target>): LaunchDecision {
        val offerable =
            chosen.filter { packageChecker.isInstalled(it.packageId) && buildUri(request, it) != null }
        return when {
            offerable.size > 1 ->
                LaunchDecision.ShowChooser(offerable, request.type == LaunchRequest.TYPE_SEARCH)
            offerable.size == 1 -> choose(request, offerable.single())
            // `choose` still answers Finish when there is no link to show the nudge for.
            else -> choose(request, chosen.firstOrNull() ?: Target.STREMIO)
        }
    }

    /** Answers a chooser pick, and is the whole of [decide] once the target is settled. */
    fun choose(request: LaunchRequest, target: Target): LaunchDecision {
        val uri = buildUri(request, target) ?: return LaunchDecision.Finish
        return if (packageChecker.isInstalled(target.packageId)) {
            LaunchDecision.Launch(target, uri)
        } else {
            LaunchDecision.ShowMissing(target, request.title)
        }
    }

    private fun buildUri(request: LaunchRequest, target: Target): String? {
        val imdbId = request.imdbId?.takeUnless { it.isBlank() }
        val title = request.title?.takeUnless { it.isBlank() }
        val isEpisode =
            request.type == LaunchRequest.TYPE_SERIES && request.season >= 0 && request.episode >= 0
        return when (target) {
            // Stremio addresses a title by IMDb id alone, so without one there is nothing to
            // link to but a search.
            Target.STREMIO ->
                when {
                    request.type == LaunchRequest.TYPE_MOVIE && imdbId != null ->
                        StremioLinks.movie(imdbId, autoPlay = isTv)

                    isEpisode && imdbId != null ->
                        StremioLinks.seriesEpisode(
                            imdbId,
                            request.season,
                            request.episode,
                            autoPlay = isTv,
                        )

                    // Explicit search requests, and the defensive fallback for anything
                    // malformed that still carries a title.
                    title != null -> StremioLinks.search(title)

                    else -> null
                }

            // Fireguy matches on the name as well as the id, so a title is enough on its own.
            // Season and episode are read wherever they are present rather than only on a
            // TYPE_SERIES request: a title with no IMDb id is published as a search, and those
            // numbers are exactly what lets Fireguy still answer with the episode itself. The
            // year is the same story for a work whose namesake from another year is also carried.
            Target.FIREGUY ->
                when {
                    title == null -> null
                    request.season >= 0 && request.episode >= 0 ->
                        FireguyLinks.title(title, imdbId, request.season, request.episode, request.year)

                    else -> FireguyLinks.title(title, imdbId, year = request.year)
                }
        }
    }
}
