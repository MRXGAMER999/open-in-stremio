package io.github.mrxgamer999.openinstremio.deeplink

import org.junit.Assert.assertEquals
import org.junit.Test

class FireguyLinksTest {

    @Test
    fun title_buildsNameOnlyLink() {
        assertEquals("fireguy://title?name=Arcane", FireguyLinks.title("Arcane"))
    }

    @Test
    fun title_carriesTheImdbIdAfterTheName() {
        assertEquals(
            "fireguy://title?name=The%20Godfather&imdb=tt0068646",
            FireguyLinks.title("The Godfather", imdbId = "tt0068646"),
        )
    }

    @Test
    fun title_ignoresABlankImdbId() {
        assertEquals("fireguy://title?name=Arcane", FireguyLinks.title("Arcane", imdbId = "  "))
    }

    @Test
    fun title_appendsSeasonAndEpisode() {
        assertEquals(
            "fireguy://title?name=Friends&imdb=tt0108778&season=1&episode=1",
            FireguyLinks.title("Friends", imdbId = "tt0108778", season = 1, episode = 1),
        )
    }

    @Test
    fun title_carriesTheYearAfterTheImdbId() {
        // Fireguy's name fallback cannot tell a 2026 film from its 2002 namesake without this.
        assertEquals(
            "fireguy://title?name=Resident%20Evil&imdb=tt1234567&year=2026",
            FireguyLinks.title("Resident Evil", imdbId = "tt1234567", year = 2026),
        )
    }

    @Test
    fun title_carriesTheYearWithoutAnImdbId() {
        assertEquals(
            "fireguy://title?name=Resident%20Evil&year=2026&season=1&episode=1",
            FireguyLinks.title("Resident Evil", season = 1, episode = 1, year = 2026),
        )
    }

    @Test
    fun title_needsBothNumbers_orNeither() {
        // A season with no episode addresses nothing Fireguy can act on, so it is not sent.
        assertEquals("fireguy://title?name=Friends", FireguyLinks.title("Friends", season = 1))
        assertEquals("fireguy://title?name=Friends", FireguyLinks.title("Friends", episode = 1))
    }

    @Test
    fun name_encodesSpacesAsPercent20() {
        assertEquals("fireguy://title?name=Breaking%20Bad", FireguyLinks.title("Breaking Bad"))
    }

    @Test
    fun name_encodesReservedCharacters() {
        assertEquals(
            "fireguy://title?name=Law%20%26%20Order%3A%20SVU",
            FireguyLinks.title("Law & Order: SVU"),
        )
    }
}
