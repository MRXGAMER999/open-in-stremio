package io.github.mrxgamer999.openinstremio.data.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

/**
 * Minimal GitHub REST client for the manual update check.
 * Note: returns HTTP 404 when the repository has no published releases.
 */
interface GitHubService {

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(@Path("owner") owner: String, @Path("repo") repo: String): ReleaseDto
}

@Serializable
data class ReleaseDto(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    val body: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
)
