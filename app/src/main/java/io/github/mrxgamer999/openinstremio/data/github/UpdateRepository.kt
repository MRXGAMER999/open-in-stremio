package io.github.mrxgamer999.openinstremio.data.github

import io.github.mrxgamer999.openinstremio.data.AppLinks
import retrofit2.HttpException

interface UpdateRepository {
    suspend fun fetchLatest(): LatestReleaseResult
}

sealed interface LatestReleaseResult {
    data class Release(
        val version: String,
        val url: String,
        val notes: List<String>,
        val publishedAt: String?,
    ) : LatestReleaseResult

    /** The repository has no published releases yet (GitHub answers 404). */
    data object NoReleases : LatestReleaseResult

    /** Network failure, rate limit, or any other unexpected answer. */
    data object Error : LatestReleaseResult
}

class DefaultUpdateRepository(
    private val service: GitHubService,
    private val owner: String = AppLinks.REPO_OWNER,
    private val repo: String = AppLinks.REPO_NAME,
) : UpdateRepository {

    override suspend fun fetchLatest(): LatestReleaseResult =
        try {
            val release = service.latestRelease(owner, repo)
            LatestReleaseResult.Release(
                version = release.tagName,
                url = release.htmlUrl,
                notes = parseNotes(release.body),
                publishedAt = release.publishedAt,
            )
        } catch (e: HttpException) {
            if (e.code() == 404) LatestReleaseResult.NoReleases else LatestReleaseResult.Error
        } catch (e: Exception) {
            LatestReleaseResult.Error
        }

    private companion object {
        const val MAX_NOTES = 6

        /** Pulls the bullet lines out of a markdown release body for the "What's new" card. */
        fun parseNotes(body: String?): List<String> =
            body.orEmpty()
                .lines()
                .map { it.trim() }
                .filter { it.startsWith("- ") || it.startsWith("* ") }
                .map { it.removePrefix("- ").removePrefix("* ").trim() }
                .filter { it.isNotEmpty() }
                .take(MAX_NOTES)
    }
}
