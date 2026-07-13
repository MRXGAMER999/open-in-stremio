package io.github.mrxgamer999.openinstremio.forwarder

import io.github.mrxgamer999.openinstremio.data.PackageChecker
import org.junit.Assert.assertEquals
import org.junit.Test

class StremioLaunchViewModelTest {

    private val stremioInstalled = PackageChecker { true }
    private val stremioMissing = PackageChecker { false }

    private fun request(
        type: String? = null,
        imdbId: String? = null,
        season: Int = -1,
        episode: Int = -1,
        title: String? = null,
    ) = LaunchRequest(type, imdbId, season, episode, title)

    @Test
    fun movie_withStremioInstalled_launchesDetailLink() {
        val viewModel = StremioLaunchViewModel(stremioInstalled, isTv = false)

        val decision = viewModel.decide(request(type = "movie", imdbId = "tt0068646"))

        assertEquals(
            LaunchDecision.LaunchStremio("stremio:///detail/movie/tt0068646/tt0068646"),
            decision,
        )
    }

    @Test
    fun series_withStremioInstalled_launchesEpisodeLink() {
        val viewModel = StremioLaunchViewModel(stremioInstalled, isTv = false)

        val decision =
            viewModel.decide(request(type = "series", imdbId = "tt0108778", season = 1, episode = 1))

        assertEquals(
            LaunchDecision.LaunchStremio("stremio:///detail/series/tt0108778/tt0108778:1:1"),
            decision,
        )
    }

    @Test
    fun onTv_appendsAutoPlay() {
        val viewModel = StremioLaunchViewModel(stremioInstalled, isTv = true)

        val decision = viewModel.decide(request(type = "movie", imdbId = "tt0068646"))

        assertEquals(
            LaunchDecision.LaunchStremio("stremio:///detail/movie/tt0068646/tt0068646?autoPlay=true"),
            decision,
        )
    }

    @Test
    fun search_launchesSearchLink() {
        val viewModel = StremioLaunchViewModel(stremioInstalled, isTv = false)

        val decision = viewModel.decide(request(type = "search", title = "Breaking Bad"))

        assertEquals(
            LaunchDecision.LaunchStremio("stremio:///search?search=Breaking%20Bad"),
            decision,
        )
    }

    @Test
    fun malformedSeriesRequest_withTitle_fallsBackToSearch() {
        val viewModel = StremioLaunchViewModel(stremioInstalled, isTv = false)

        // Missing episode number: not enough for a detail link, but the title is usable.
        val decision =
            viewModel.decide(request(type = "series", imdbId = "tt0108778", season = 1, title = "Friends"))

        assertEquals(LaunchDecision.LaunchStremio("stremio:///search?search=Friends"), decision)
    }

    @Test
    fun stremioMissing_showsDialogWithTitle() {
        val viewModel = StremioLaunchViewModel(stremioMissing, isTv = false)

        val decision =
            viewModel.decide(request(type = "movie", imdbId = "tt0068646", title = "The Godfather"))

        assertEquals(LaunchDecision.ShowMissingDialog("The Godfather"), decision)
    }

    @Test
    fun nothingUsable_finishes() {
        val viewModel = StremioLaunchViewModel(stremioInstalled, isTv = false)

        assertEquals(LaunchDecision.Finish, viewModel.decide(request()))
        assertEquals(LaunchDecision.Finish, viewModel.decide(request(type = "movie", title = "  ")))
    }
}
