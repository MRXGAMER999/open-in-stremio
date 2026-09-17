package io.github.mrxgamer999.openinstremio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mrxgamer999.openinstremio.data.ExtensionStatusRepository
import io.github.mrxgamer999.openinstremio.data.PlayerChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(repository: ExtensionStatusRepository, choice: Flow<PlayerChoice>) : ViewModel() {

  val uiState: StateFlow<HomeUiState> =
    combine<_, _, HomeUiState>(repository.status, choice) { status, chosen ->
        HomeUiState.Ready(
          statusVariant =
            when {
              !status.seriesGuideInstalled -> StatusVariant.SERIESGUIDE_MISSING
              status.extensionActive -> StatusVariant.ACTIVE
              else -> StatusVariant.NOT_ENABLED
            },
          choice = chosen,
          stremioInstalled = status.stremioInstalled,
          fireguyInstalled = status.fireguyInstalled,
        )
      }
      .catch {
        emit(
          HomeUiState.Ready(
            StatusVariant.NOT_ENABLED,
            PlayerChoice.BOTH,
            stremioInstalled = false,
            fireguyInstalled = false,
          )
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState.Loading)
}

sealed interface HomeUiState {
  data object Loading : HomeUiState

  data class Ready(
    val statusVariant: StatusVariant,
    /** Which apps the button opens; Home shows it and links to Settings to change it. */
    val choice: PlayerChoice,
    val stremioInstalled: Boolean,
    val fireguyInstalled: Boolean,
  ) : HomeUiState
}

enum class StatusVariant {
  /** Extension is enabled inside SeriesGuide - everything is working. */
  ACTIVE,
  /** SeriesGuide is installed but the extension hasn't been switched on yet. */
  NOT_ENABLED,
  /** SeriesGuide isn't installed on this device. */
  SERIESGUIDE_MISSING,
}
