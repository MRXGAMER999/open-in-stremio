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
     * The chooser only appears when it has something to choose between. With one target
     * installed the tap goes straight there, and with none it lands on Stremio's install
     * nudge - the behaviour from before Fireguy existed, for the people who only ever had
     * Stremio in mind.
     */
    fun decide(request: LaunchRequest): LaunchDecision {
        val offerable = packageChecker.installedTargets().filter { buildUri(request, it) != null }
        return when {
            offerable.size > 1 ->
                LaunchDecision.ShowChooser(offerable, request.type == LaunchRequest.TYPE_SEARCH)
            offerable.size == 1 -> choose(request, offerable.single())
            // Nothing installed, or nothing this request can address anywhere: Stremio's
            // install nudge is what a tap did before Fireguy existed, and `choose` still
            // answers Finish when there is no link to show it for.
            else -> choose(request, Target.STREMIO)
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
            // numbers are exactly what lets Fireguy still answer with the episode itself.
            Target.FIREGUY ->
                when {
                    title == null -> null
                    request.season >= 0 && request.episode >= 0 ->
                        FireguyLinks.title(title, imdbId, request.season, request.episode)

                    else -> FireguyLinks.title(title, imdbId)
                }
        }
    }
}
