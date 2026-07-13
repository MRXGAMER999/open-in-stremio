package io.github.mrxgamer999.openinstremio.data.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDb v3 endpoints used to look up an IMDb id when SeriesGuide does not provide one.
 * https://developer.themoviedb.org/reference/movie-external-ids
 * https://developer.themoviedb.org/reference/tv-series-external-ids
 */
interface TmdbService {

    @GET("movie/{id}/external_ids")
    suspend fun movieExternalIds(
        @Path("id") tmdbId: Int,
        @Query("api_key") apiKey: String,
    ): ExternalIdsDto

    @GET("tv/{id}/external_ids")
    suspend fun tvExternalIds(
        @Path("id") tmdbId: Int,
        @Query("api_key") apiKey: String,
    ): ExternalIdsDto
}

@Serializable
data class ExternalIdsDto(
    @SerialName("imdb_id") val imdbId: String? = null,
)
