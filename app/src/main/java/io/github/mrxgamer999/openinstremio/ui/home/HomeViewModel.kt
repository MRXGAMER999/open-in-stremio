package io.github.mrxgamer999.openinstremio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mrxgamer999.openinstremio.data.ExtensionStatusRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(repository: ExtensionStatusRepository) : ViewModel() {

  val uiState: StateFlow<HomeUiState> =
    repository.status
      .map<_, HomeUiState> { status ->
        HomeUiState.Ready(
          statusVariant =
            when {
              !status.seriesGuideInstalled -> StatusVariant.SERIESGUIDE_MISSING
              status.extensionActive -> StatusVariant.ACTIVE
              else -> StatusVariant.NOT_ENABLED
            }
        )
      }
      .catch { emit(HomeUiState.Ready(StatusVariant.NOT_ENABLED)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState.Loading)
}

sealed interface HomeUiState {
  data object Loading : HomeUiState

  data class Ready(val statusVariant: StatusVariant) : HomeUiState
}

enum class StatusVariant {
  /** Extension is enabled inside SeriesGuide - everything is working. */
  ACTIVE,
  /** SeriesGuide is installed but the extension hasn't been switched on yet. */
  NOT_ENABLED,
  /** SeriesGuide isn't installed on this device. */
  SERIESGUIDE_MISSING,
}
