package io.github.mrxgamer999.openinstremio.ui.setup

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mrxgamer999.openinstremio.R
import io.github.mrxgamer999.openinstremio.data.PackageChecker
import io.github.mrxgamer999.openinstremio.data.Packages
import io.github.mrxgamer999.openinstremio.data.PlayerChoice
import io.github.mrxgamer999.openinstremio.data.PlayerChoiceStore
import io.github.mrxgamer999.openinstremio.forwarder.actionLabelRes
import io.github.mrxgamer999.openinstremio.forwarder.installedTargets
import io.github.mrxgamer999.openinstremio.forwarder.targets
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SetupGuideViewModel(
  private val store: PlayerChoiceStore,
  private val packageChecker: PackageChecker,
) : ViewModel() {

  val uiState: StateFlow<SetupGuideUiState> =
    store.choice
      .map { setupGuideState(it, packageChecker) }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        setupGuideState(PlayerChoice.BOTH, packageChecker),
      )

  fun choose(choice: PlayerChoice) {
    viewModelScope.launch { store.set(choice) }
  }
}

/**
 * The guide's pages for [choice]. Always the same five, so the pager never changes length under
 * the user; only what the install page and the last page say depends on the choice.
 */
internal fun setupGuideState(choice: PlayerChoice, packageChecker: PackageChecker): SetupGuideUiState {
  val stremioInstalled = packageChecker.isInstalled(Packages.STREMIO)
  val fireguyInstalled = packageChecker.isInstalled(Packages.FIREGUY)
  return SetupGuideUiState(
    choice = choice,
    stremioInstalled = stremioInstalled,
    fireguyInstalled = fireguyInstalled,
    pages =
      listOf(
        SetupPage(
          title = R.string.setup_1_title,
          body = R.string.setup_1_body,
          action = null,
          illustration = SetupIllustration.WELCOME,
        ),
        SetupPage(
          title = R.string.setup_choice_title,
          body = R.string.setup_choice_body,
          action = SetupPageAction.CHOOSE_PLAYERS,
          illustration = null,
        ),
        SetupPage(
          title = R.string.setup_2_title,
          body = R.string.setup_2_body,
          action = SetupPageAction.OPEN_SG_EXTENSIONS,
          illustration = SetupIllustration.SERIESGUIDE,
        ),
        SetupPage(
          title = R.string.setup_3_title,
          body =
            when (choice) {
              PlayerChoice.STREMIO -> R.string.setup_3_body_stremio
              PlayerChoice.FIREGUY -> R.string.setup_3_body_fireguy
              PlayerChoice.BOTH -> R.string.setup_3_body_both
            },
          // Fireguy is sideloaded, so there is no store page to offer for it.
          action = if (choice == PlayerChoice.FIREGUY) null else SetupPageAction.GET_STREMIO,
          illustration = SetupIllustration.INSTALL,
        ),
        SetupPage(
          title = R.string.setup_4_title,
          body = R.string.setup_4_body,
          action = null,
          illustration = SetupIllustration.DONE,
          // Names the button exactly as SeriesGuide will show it on this device.
          bodyArg = actionLabelRes(choice.targets, packageChecker.installedTargets(), open = true),
        ),
      ),
  )
}

data class SetupGuideUiState(
  val pages: List<SetupPage>,
  val choice: PlayerChoice,
  val stremioInstalled: Boolean,
  val fireguyInstalled: Boolean,
)

data class SetupPage(
  @param:StringRes val title: Int,
  @param:StringRes val body: Int,
  val action: SetupPageAction?,
  /** Null on the choice page, which needs the room for its options. */
  val illustration: SetupIllustration?,
  /** A string resource formatted into [body], when it has a placeholder. */
  @param:StringRes val bodyArg: Int? = null,
)

enum class SetupPageAction {
  CHOOSE_PLAYERS,
  OPEN_SG_EXTENSIONS,
  GET_STREMIO,
}

enum class SetupIllustration {
  WELCOME,
  SERIESGUIDE,
  INSTALL,
  DONE,
}
