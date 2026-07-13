package io.github.mrxgamer999.openinstremio.data.github

import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class UpdateRepositoryTest {

    private class FakeGitHubService(val result: () -> ReleaseDto) : GitHubService {
        override suspend fun latestRelease(owner: String, repo: String): ReleaseDto = result()
    }

    private fun http404(): HttpException =
        HttpException(Response.error<ReleaseDto>(404, "".toResponseBody()))

    private fun http403(): HttpException =
        HttpException(Response.error<ReleaseDto>(403, "".toResponseBody()))

    @Test
    fun successfulRelease_mapsFieldsAndParsesBulletNotes() = runTest {
        val service =
            FakeGitHubService {
                ReleaseDto(
                    tagName = "v1.1.0",
                    htmlUrl = "https://github.com/MRXGAMER999/open-in-stremio/releases/tag/v1.1.0",
                    body = "Intro line\n- Faster IMDb matching\n* Clearer errors\n\nOutro",
                    publishedAt = "2026-07-01T00:00:00Z",
                )
            }
        val result = DefaultUpdateRepository(service).fetchLatest()

        assertEquals(
            LatestReleaseResult.Release(
                version = "v1.1.0",
                url = "https://github.com/MRXGAMER999/open-in-stremio/releases/tag/v1.1.0",
                notes = listOf("Faster IMDb matching", "Clearer errors"),
                publishedAt = "2026-07-01T00:00:00Z",
            ),
            result,
        )
    }

    @Test
    fun emptyBody_yieldsEmptyNotes() = runTest {
        val service = FakeGitHubService { ReleaseDto(tagName = "v1.1.0", htmlUrl = "u", body = null) }
        val result = DefaultUpdateRepository(service).fetchLatest() as LatestReleaseResult.Release

        assertEquals(emptyList<String>(), result.notes)
    }

    @Test
    fun http404_meansNoReleases() = runTest {
        val service = FakeGitHubService { throw http404() }

        assertEquals(LatestReleaseResult.NoReleases, DefaultUpdateRepository(service).fetchLatest())
    }

    @Test
    fun http403_andIoException_meanError() = runTest {
        val rateLimited = FakeGitHubService { throw http403() }
        val offline = FakeGitHubService { throw IOException("offline") }

        assertEquals(LatestReleaseResult.Error, DefaultUpdateRepository(rateLimited).fetchLatest())
        assertEquals(LatestReleaseResult.Error, DefaultUpdateRepository(offline).fetchLatest())
    }

    @Test
    fun releaseJson_withUnknownFields_decodes() {
        val json = Json { ignoreUnknownKeys = true }
        val dto =
            json.decodeFromString<ReleaseDto>(
                """
                {
                  "url": "https://api.github.com/repos/o/r/releases/1",
                  "tag_name": "v1.2.3",
                  "html_url": "https://github.com/o/r/releases/tag/v1.2.3",
                  "draft": false,
                  "prerelease": false,
                  "published_at": "2026-06-30T12:00:00Z",
                  "assets": [{"name": "app.apk", "size": 1234}],
                  "body": "- One\n- Two"
                }
                """
            )

        assertEquals("v1.2.3", dto.tagName)
        assertEquals("https://github.com/o/r/releases/tag/v1.2.3", dto.htmlUrl)
        assertEquals("- One\n- Two", dto.body)
    }
}
