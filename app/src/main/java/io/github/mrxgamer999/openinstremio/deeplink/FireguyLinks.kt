package io.github.mrxgamer999.openinstremio.deeplink

import java.net.URLEncoder

/**
 * Builder for Fireguy On Demand deep links. There is one link shape - `fireguy://title` with
 * everything in the query - because Fireguy resolves a title against its own catalogue rather
 * than addressing it by a route, so a movie, an episode and a search are the same gesture there.
 *
 * `name` rides along even when an IMDb id is known. Fireguy's id column is blanked and re-derived
 * inside every catalogue refresh, and only rows its metadata feed could match carry one at all,
 * so an id-only link would miss for reasons the caller cannot see. A title Fireguy does not carry
 * lands on its search screen with the name filled in.
 *
 * Pure Kotlin (no Android types) so it is testable on the JVM.
 */
object FireguyLinks {

    /**
     * `fireguy://title?name={name}[&imdb={imdbId}][&year={year}][&season={season}&episode={episode}]`
     *
     * Season and episode are only spelled out together: either one alone addresses nothing.
     *
     * The year rides along because the name is more than a pre-fill: it is also Fireguy's fallback
     * match when the id misses, and the year is what keeps that match off a same-named work from
     * another year (a 2026 film rather than its 2002 namesake).
     */
    fun title(
        name: String,
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        year: Int? = null,
    ): String =
        buildString {
            append("fireguy://title?name=").append(encode(name))
            imdbId?.takeUnless { it.isBlank() }?.let { append("&imdb=").append(it) }
            year?.let { append("&year=").append(it) }
            if (season != null && episode != null) append("&season=$season&episode=$episode")
        }

    /**
     * Fireguy reads its query parameters still percent-encoded and decodes them itself, so the
     * value has to arrive encoded. `+` is spelled `%20` for the same reason Stremio's search link
     * does it: the decoder on the other side reads a `+` as a space.
     */
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
