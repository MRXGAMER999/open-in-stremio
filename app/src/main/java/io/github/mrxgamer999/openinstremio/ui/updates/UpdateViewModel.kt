package io.github.mrxgamer999.openinstremio.ui.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mrxgamer999.openinstremio.data.github.LatestReleaseResult
import io.github.mrxgamer999.openinstremio.data.github.UpdateRepository
import io.github.mrxgamer999.openinstremio.util.VersionComparator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(
  private val repository: UpdateRepository,
  private val currentVersion: String,
) : ViewModel() {

  private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Checking)
  val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

  init {
    check()
  }

  fun check() {
    _uiState.value = UpdateUiState.Checking
    viewModelScope.launch {
      _uiState.value =
        when (val result = repository.fetchLatest()) {
          is LatestReleaseResult.Release ->
            if (VersionComparator.isNewer(current = currentVersion, latest = result.version)) {
              UpdateUiState.UpdateAvailable(
                currentVersion = currentVersion,
                latestVersion = result.version,
                notes = result.notes,
                url = result.url,
              )
            } else {
              UpdateUiState.UpToDate(currentVersion)
            }
          LatestReleaseResult.NoReleases -> UpdateUiState.NoReleases
          LatestReleaseResult.Error -> UpdateUiState.Error
        }
    }
  }
}

sealed interface UpdateUiState {
  data object Checking : UpdateUiState

  data class UpToDate(val currentVersion: String) : UpdateUiState

  data class UpdateAvailable(
    val currentVersion: String,
    val latestVersion: String,
    val notes: List<String>,
    val url: String,
  ) : UpdateUiState

  data object NoReleases : UpdateUiState

  data object Error : UpdateUiState
}
