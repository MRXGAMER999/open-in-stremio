package io.github.mrxgamer999.openinstremio.ui.about

import androidx.lifecycle.ViewModel
import io.github.mrxgamer999.openinstremio.data.AppLinks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AboutViewModel(appVersion: String) : ViewModel() {

  val uiState: StateFlow<AboutUiState> =
    MutableStateFlow(
        AboutUiState(
          version = appVersion,
          repoUrl = AppLinks.REPO_URL,
          licenseUrl = AppLinks.LICENSE_URL,
          developerUrl = AppLinks.DEVELOPER_URL,
        )
      )
      .asStateFlow()
}

data class AboutUiState(
  val version: String,
  val repoUrl: String,
  val licenseUrl: String,
  val developerUrl: String,
)
