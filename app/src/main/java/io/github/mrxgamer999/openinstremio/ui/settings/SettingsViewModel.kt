package io.github.mrxgamer999.openinstremio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mrxgamer999.openinstremio.data.PackageChecker
import io.github.mrxgamer999.openinstremio.data.Packages
import io.github.mrxgamer999.openinstremio.data.PlayerChoice
import io.github.mrxgamer999.openinstremio.data.PlayerChoiceStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
  private val store: PlayerChoiceStore,
  private val packageChecker: PackageChecker,
) : ViewModel() {

  val uiState: StateFlow<SettingsUiState> =
    store.choice
      .map<_, SettingsUiState> { choice ->
        SettingsUiState.Ready(
          choice = choice,
          stremioInstalled = packageChecker.isInstalled(Packages.STREMIO),
          fireguyInstalled = packageChecker.isInstalled(Packages.FIREGUY),
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState.Loading)

  fun choose(choice: PlayerChoice) {
    viewModelScope.launch { store.set(choice) }
  }
}

sealed interface SettingsUiState {
  data object Loading : SettingsUiState

  data class Ready(
    val choice: PlayerChoice,
    val stremioInstalled: Boolean,
    val fireguyInstalled: Boolean,
  ) : SettingsUiState
}
