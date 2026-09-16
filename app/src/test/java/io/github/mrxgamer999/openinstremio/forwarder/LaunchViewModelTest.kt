package io.github.mrxgamer999.openinstremio.forwarder

import io.github.mrxgamer999.openinstremio.data.PackageChecker
import io.github.mrxgamer999.openinstremio.data.Packages
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchViewModelTest {

    private fun installed(vararg packages: String) = PackageChecker { it in packages }

    private val stremioOnly = installed(Packages.STREMIO)
    private val fireguyOnly = installed(Packages.FIREGUY)
    private val bothInstalled = installed(Packages.STREMIO, Packages.FIREGUY)
    private val noneInstalled = installed()

    private fun request(
        type: String? = null,
        imdbId: String? = null,
        season: Int = -1,
        episode: Int = -1,
        title: String? = null,
    ) = LaunchRequest(type, imdbId, season, episode, title)

    @Test
    fun movie_withStremioInstalled_launchesDetailLink() {
        val viewModel = LaunchViewModel(stremioOnly, isTv = false)

        val decision = viewModel.decide(request(type = "movie", imdbId = "tt0068646"))

        assertEquals(
            LaunchDecision.Launch(Target.STREMIO, "stremio:///detail/movie/tt0068646/tt0068646"),
            decision,
        )
    }

    @Test
    fun series_withStremioInstalled_launchesEpisodeLink() {
        val viewModel = LaunchViewModel(stremioOnly, isTv = false)

        val decision =
            viewModel.decide(request(type = "series", imdbId = "tt0108778", season = 1, episode = 1))

        assertEquals(
            LaunchDecision.Launch(Target.STREMIO, "stremio:///detail/series/tt0108778/tt0108778:1:1"),
            decision,
        )
    }

    @Test
    fun onTv_appendsAutoPlay() {
        val viewModel = LaunchViewModel(stremioOnly, isTv = true)

        val decision = viewModel.decide(request(type = "movie", imdbId = "tt0068646"))

        assertEquals(
            LaunchDecision.Launch(
                Target.STREMIO,
                "stremio:///detail/movie/tt0068646/tt0068646?autoPlay=true",
            ),
            decision,
        )
    }

    @Test
    fun search_launchesSearchLink() {
        val viewModel = LaunchViewModel(stremioOnly, isTv = false)

        val decision = viewModel.decide(request(type = "search", title = "Breaking Bad"))

        assertEquals(
            LaunchDecision.Launch(Target.STREMIO, "stremio:///search?search=Breaking%20Bad"),
            decision,
        )
    }

    @Test
    fun malformedSeriesRequest_withTitle_fallsBackToSearch() {
        val viewModel = LaunchViewModel(stremioOnly, isTv = false)

        // Missing episode number: not enough for a detail link, but the title is usable.
        val decision =
            viewModel.decide(request(type = "series", imdbId = "tt0108778", season = 1, title = "Friends"))

        assertEquals(
            LaunchDecision.Launch(Target.STREMIO, "stremio:///search?search=Friends"),
            decision,
        )
    }

    @Test
    fun stremioMissing_showsDialogWithTitle() {
        val viewModel = LaunchViewModel(noneInstalled, isTv = false)

        val decision =
            viewModel.decide(request(type = "movie", imdbId = "tt0068646", title = "The Godfather"))

        assertEquals(LaunchDecision.ShowMissing(Target.STREMIO, "The Godfather"), decision)
    }

    @Test
    fun nothingUsable_finishes() {
        val viewModel = LaunchViewModel(stremioOnly, isTv = false)

        assertEquals(LaunchDecision.Finish, viewModel.decide(request()))
        assertEquals(LaunchDecision.Finish, viewModel.decide(request(type = "movie", title = "  ")))
    }

    @Test
    fun bothInstalled_showsChooserOverBothTargets() {
        val viewModel = LaunchViewModel(bothInstalled, isTv = false)

        val decision =
            viewModel.decide(request(type = "movie", imdbId = "tt0068646", title = "The Godfather"))

        assertEquals(
            LaunchDecision.ShowChooser(listOf(Target.STREMIO, Target.FIREGUY), isSearch = false),
            decision,
        )
    }

    @Test
    fun bothInstalled_searchFallback_showsAChooserThatSaysSearch() {
        val viewModel = LaunchViewModel(bothInstalled, isTv = false)

        // The published button said "Search in…" for this title; the rows must not promise to
        // open something that has no id to open.
        val decision = viewModel.decide(request(type = "search", title = "Some Very New Show"))

        assertEquals(
            LaunchDecision.ShowChooser(listOf(Target.STREMIO, Target.FIREGUY), isSearch = true),
            decision,
        )
    }

    @Test
    fun bothInstalled_butOnlyOneCanOpenIt_skipsTheChooser() {
        val viewModel = LaunchViewModel(bothInstalled, isTv = false)

        // No title, so Fireguy has nothing to match on and only Stremio can answer.
        val decision = viewModel.decide(request(type = "movie", imdbId = "tt0068646"))

        assertEquals(
            LaunchDecision.Launch(Target.STREMIO, "stremio:///detail/movie/tt0068646/tt0068646"),
            decision,
        )
    }

    @Test
    fun onlyFireguyInstalled_launchesFireguyWithoutAsking() {
        val viewModel = LaunchViewModel(fireguyOnly, isTv = false)

        val decision =
            viewModel.decide(request(type = "movie", imdbId = "tt0068646", title = "The Godfather"))

        assertEquals(
            LaunchDecision.Launch(
                Target.FIREGUY,
                "fireguy://title?name=The%20Godfather&imdb=tt0068646",
            ),
            decision,
        )
    }

    @Test
    fun chooseFireguy_buildsEpisodeLink() {
        val viewModel = LaunchViewModel(bothInstalled, isTv = false)

        val decision =
            viewModel.choose(
                request(type = "series", imdbId = "tt0108778", season = 1, episode = 1, title = "Friends"),
                Target.FIREGUY,
            )

        assertEquals(
            LaunchDecision.Launch(
                Target.FIREGUY,
                "fireguy://title?name=Friends&imdb=tt0108778&season=1&episode=1",
            ),
            decision,
        )
    }

    @Test
    fun chooseFireguy_whenMissing_showsFireguyDialog() {
        val viewModel = LaunchViewModel(stremioOnly, isTv = false)

        val decision =
            viewModel.choose(
                request(type = "movie", imdbId = "tt0068646", title = "The Godfather"),
                Target.FIREGUY,
            )

        assertEquals(LaunchDecision.ShowMissing(Target.FIREGUY, "The Godfather"), decision)
    }

    @Test
    fun fireguy_searchFallbackForAnEpisode_stillReachesTheEpisode() {
        val viewModel = LaunchViewModel(fireguyOnly, isTv = false)

        // No IMDb id, so the receiver published this as a search - but the numbers came along,
        // and Fireguy matches on the name, so it can still answer with the episode.
        val decision =
            viewModel.decide(request(type = "search", season = 2, episode = 5, title = "Friends"))

        assertEquals(
            LaunchDecision.Launch(Target.FIREGUY, "fireguy://title?name=Friends&season=2&episode=5"),
            decision,
        )
    }

    @Test
    fun fireguy_withoutAnImdbId_stillLinksOnTheName() {
        val viewModel = LaunchViewModel(fireguyOnly, isTv = false)

        val decision = viewModel.decide(request(type = "search", title = "Law & Order: SVU"))

        assertEquals(
            LaunchDecision.Launch(
                Target.FIREGUY,
                "fireguy://title?name=Law%20%26%20Order%3A%20SVU",
            ),
            decision,
        )
    }
}
