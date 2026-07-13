package io.github.mrxgamer999.openinstremio.ui.updates

import io.github.mrxgamer999.openinstremio.data.github.LatestReleaseResult
import io.github.mrxgamer999.openinstremio.data.github.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeUpdateRepository(var result: LatestReleaseResult) : UpdateRepository {
        override suspend fun fetchLatest(): LatestReleaseResult = result
    }

    private fun release(version: String) =
        LatestReleaseResult.Release(
            version = version,
            url = "https://example.com/release",
            notes = listOf("Note"),
            publishedAt = null,
        )

    @Test
    fun startsChecking_thenUpdateAvailable_whenLatestIsNewer() = runTest {
        val viewModel = UpdateViewModel(FakeUpdateRepository(release("v1.1.0")), currentVersion = "1.0.0")

        assertEquals(UpdateUiState.Checking, viewModel.uiState.value)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            UpdateUiState.UpdateAvailable(
                currentVersion = "1.0.0",
                latestVersion = "v1.1.0",
                notes = listOf("Note"),
                url = "https://example.com/release",
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun upToDate_whenLatestEqualsCurrent() = runTest {
        val viewModel = UpdateViewModel(FakeUpdateRepository(release("v1.0.0")), currentVersion = "1.0.0")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(UpdateUiState.UpToDate("1.0.0"), viewModel.uiState.value)
    }

    @Test
    fun noReleases_and_error_mapToTheirStates() = runTest {
        val repository = FakeUpdateRepository(LatestReleaseResult.NoReleases)
        val viewModel = UpdateViewModel(repository, currentVersion = "1.0.0")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(UpdateUiState.NoReleases, viewModel.uiState.value)

        repository.result = LatestReleaseResult.Error
        viewModel.check()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(UpdateUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun retryAfterError_recovers() = runTest {
        val repository = FakeUpdateRepository(LatestReleaseResult.Error)
        val viewModel = UpdateViewModel(repository, currentVersion = "1.0.0")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(UpdateUiState.Error, viewModel.uiState.value)

        repository.result = release("v2.0.0")
        viewModel.check()
        assertEquals(UpdateUiState.Checking, viewModel.uiState.value)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            UpdateUiState.UpdateAvailable("1.0.0", "v2.0.0", listOf("Note"), "https://example.com/release"),
            viewModel.uiState.value,
        )
    }
}
