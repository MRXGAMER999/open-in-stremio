package io.github.mrxgamer999.openinstremio.forwarder

import androidx.lifecycle.ViewModel
import io.github.mrxgamer999.openinstremio.data.PackageChecker
import io.github.mrxgamer999.openinstremio.data.Packages
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
    /** Stremio is installed: fire this deep link and finish. */
    data class LaunchStremio(val uri: String) : LaunchDecision

    /** Stremio is missing: show the install dialog from the design. */
    data class ShowMissingDialog(val title: String?) : LaunchDecision

    /** Nothing usable in the intent: exit quietly. */
    data object Finish : LaunchDecision
}

/**
 * Presentation logic for [StremioLaunchActivity]: builds the deep link for the request and
 * decides between launching Stremio and showing the "Stremio isn't installed" dialog.
 * Pure and synchronous so it is unit-testable without an Activity.
 */
class StremioLaunchViewModel(
    private val packageChecker: PackageChecker,
    private val isTv: Boolean,
) : ViewModel() {

    fun decide(request: LaunchRequest): LaunchDecision {
        val uri = buildUri(request) ?: return LaunchDecision.Finish
        return if (packageChecker.isInstalled(Packages.STREMIO)) {
            LaunchDecision.LaunchStremio(uri)
        } else {
            LaunchDecision.ShowMissingDialog(request.title)
        }
    }

    private fun buildUri(request: LaunchRequest): String? {
        val imdbId = request.imdbId?.takeUnless { it.isBlank() }
        val title = request.title?.takeUnless { it.isBlank() }
        return when {
            request.type == LaunchRequest.TYPE_MOVIE && imdbId != null ->
                StremioLinks.movie(imdbId, autoPlay = isTv)

            request.type == LaunchRequest.TYPE_SERIES &&
                imdbId != null &&
                request.season >= 0 &&
                request.episode >= 0 ->
                StremioLinks.seriesEpisode(imdbId, request.season, request.episode, autoPlay = isTv)

            // Explicit search requests, and the defensive fallback for anything malformed
            // that still carries a title.
            title != null -> StremioLinks.search(title)

            else -> null
        }
    }
}
