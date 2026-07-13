package io.github.mrxgamer999.openinstremio.ui.setup

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import io.github.mrxgamer999.openinstremio.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SetupGuideViewModel : ViewModel() {

  val uiState: StateFlow<SetupGuideUiState> =
    MutableStateFlow(
        SetupGuideUiState(
          pages =
            listOf(
              SetupPage(
                title = R.string.setup_1_title,
                body = R.string.setup_1_body,
                action = null,
              ),
              SetupPage(
                title = R.string.setup_2_title,
                body = R.string.setup_2_body,
                action = SetupPageAction.OPEN_SG_EXTENSIONS,
              ),
              SetupPage(
                title = R.string.setup_3_title,
                body = R.string.setup_3_body,
                action = SetupPageAction.GET_STREMIO,
              ),
              SetupPage(
                title = R.string.setup_4_title,
                body = R.string.setup_4_body,
                action = null,
              ),
            )
        )
      )
      .asStateFlow()
}

data class SetupGuideUiState(val pages: List<SetupPage>)

data class SetupPage(
  @param:StringRes val title: Int,
  @param:StringRes val body: Int,
  val action: SetupPageAction?,
)

enum class SetupPageAction {
  OPEN_SG_EXTENSIONS,
  GET_STREMIO,
}
