package io.github.mrxgamer999.openinstremio.deeplink

import org.junit.Assert.assertEquals
import org.junit.Test

class StremioLinksTest {

    @Test
    fun movie_buildsDetailLinkWithDoubledId() {
        assertEquals(
            "stremio:///detail/movie/tt0066921/tt0066921",
            StremioLinks.movie("tt0066921"),
        )
    }

    @Test
    fun movie_withAutoPlay_appendsQueryParam() {
        assertEquals(
            "stremio:///detail/movie/tt0066921/tt0066921?autoPlay=true",
            StremioLinks.movie("tt0066921", autoPlay = true),
        )
    }

    @Test
    fun seriesEpisode_usesShowIdWithSeasonAndEpisode() {
        assertEquals(
            "stremio:///detail/series/tt0108778/tt0108778:1:1",
            StremioLinks.seriesEpisode("tt0108778", season = 1, episode = 1),
        )
    }

    @Test
    fun seriesEpisode_keepsColonsUnencoded_withAutoPlay() {
        assertEquals(
            "stremio:///detail/series/tt0903747/tt0903747:5:14?autoPlay=true",
            StremioLinks.seriesEpisode("tt0903747", season = 5, episode = 14, autoPlay = true),
        )
    }

    @Test
    fun search_encodesSpacesAsPercent20() {
        assertEquals(
            "stremio:///search?search=Breaking%20Bad",
            StremioLinks.search("Breaking Bad"),
        )
    }

    @Test
    fun search_encodesReservedCharacters() {
        assertEquals(
            "stremio:///search?search=Law%20%26%20Order%3A%20SVU",
            StremioLinks.search("Law & Order: SVU"),
        )
    }

    @Test
    fun webFallbacks_mirrorDetailPaths() {
        assertEquals(
            "https://web.stremio.com/#/detail/movie/tt0066921/tt0066921",
            StremioLinks.movieWeb("tt0066921"),
        )
        assertEquals(
            "https://web.stremio.com/#/detail/series/tt0108778/tt0108778:2:3",
            StremioLinks.seriesEpisodeWeb("tt0108778", season = 2, episode = 3),
        )
    }
}
