package io.github.mrxgamer999.openinstremio.deeplink

import java.net.URLEncoder

/**
 * Builders for Stremio deep links, per the official addon SDK docs
 * (https://github.com/Stremio/stremio-addon-sdk/blob/master/docs/deep-links.md).
 *
 * The triple slash is deliberate: `stremio:///detail/...` has an empty authority.
 * Pure Kotlin (no Android types) so it is testable on the JVM.
 */
object StremioLinks {

    private const val SCHEME = "stremio://"
    private const val WEB_BASE = "https://web.stremio.com/#"

    /** `stremio:///detail/movie/{imdbId}/{imdbId}` */
    fun movie(imdbId: String, autoPlay: Boolean = false): String =
        "$SCHEME/detail/movie/$imdbId/$imdbId" + autoPlaySuffix(autoPlay)

    /**
     * `stremio:///detail/series/{showImdbId}/{showImdbId}:{season}:{episode}`
     *
     * Series links use the show's IMDb id plus season and episode numbers; the video id
     * segment contains literal `:` separators and must not be URL-encoded.
     */
    fun seriesEpisode(showImdbId: String, season: Int, episode: Int, autoPlay: Boolean = false): String =
        "$SCHEME/detail/series/$showImdbId/$showImdbId:$season:$episode" + autoPlaySuffix(autoPlay)

    /** `stremio:///search?search={query}` */
    fun search(query: String): String =
        "$SCHEME/search?search=" + URLEncoder.encode(query, "UTF-8").replace("+", "%20")

    /** Browser fallback for the movie detail page on web.stremio.com. */
    fun movieWeb(imdbId: String): String = "$WEB_BASE/detail/movie/$imdbId/$imdbId"

    /** Browser fallback for the series episode detail page on web.stremio.com. */
    fun seriesEpisodeWeb(showImdbId: String, season: Int, episode: Int): String =
        "$WEB_BASE/detail/series/$showImdbId/$showImdbId:$season:$episode"

    /**
     * autoPlay is best-effort: Stremio only honors it in the Android TV app, and only when
     * the user has previously resolved a stream for the title. Everywhere else the link
     * simply lands on the detail page.
     */
    private fun autoPlaySuffix(autoPlay: Boolean) = if (autoPlay) "?autoPlay=true" else ""
}
