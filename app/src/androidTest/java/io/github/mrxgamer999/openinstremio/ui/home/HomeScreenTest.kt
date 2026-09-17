package io.github.mrxgamer999.openinstremio.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation3.runtime.NavKey
import io.github.mrxgamer999.openinstremio.SettingsKey
import io.github.mrxgamer999.openinstremio.SetupGuideKey
import io.github.mrxgamer999.openinstremio.data.PlayerChoice
import io.github.mrxgamer999.openinstremio.theme.OpenInStremioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun ready(variant: StatusVariant, choice: PlayerChoice = PlayerChoice.BOTH) =
        HomeUiState.Ready(variant, choice, stremioInstalled = true, fireguyInstalled = true)

    private fun setContent(uiState: HomeUiState, onNavigate: (NavKey) -> Unit = {}) {
        composeTestRule.setContent {
            OpenInStremioTheme { HomeScreen(uiState = uiState, onNavigate = onNavigate) }
        }
    }

    @Test
    fun activeState_showsHeaderAndEnabledCard() {
        setContent(ready(StatusVariant.ACTIVE))

        composeTestRule.onNodeWithText("Open in Stremio").assertIsDisplayed()
        composeTestRule.onNodeWithText("SeriesGuide → Stremio or Fireguy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Extension enabled").assertIsDisplayed()
        composeTestRule.onNodeWithText("Working with 2 apps: Stremio and Fireguy").assertIsDisplayed()
    }

    @Test
    fun seriesGuideMissingState_showsWarningCard() {
        setContent(ready(StatusVariant.SERIESGUIDE_MISSING))

        composeTestRule.onNodeWithText("SeriesGuide not installed").assertIsDisplayed()
    }

    @Test
    fun notEnabledState_showsEnableHint() {
        setContent(ready(StatusVariant.NOT_ENABLED))

        composeTestRule.onNodeWithText("Not enabled yet").assertIsDisplayed()
    }

    @Test
    fun menuRows_navigate() {
        var navigated: NavKey? = null
        setContent(ready(StatusVariant.ACTIVE)) { navigated = it }

        composeTestRule.onNodeWithText("Setup guide").performScrollTo().performClick()

        assertEquals(SetupGuideKey, navigated)
    }

    @Test
    fun singleChoice_namesItsApp_andChangeOpensSettings() {
        var navigated: NavKey? = null
        setContent(ready(StatusVariant.ACTIVE, PlayerChoice.FIREGUY)) { navigated = it }

        composeTestRule.onNodeWithText("Working with Fireguy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Opens titles in Fireguy").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Change").performScrollTo().performClick()

        assertEquals(SettingsKey, navigated)
    }
}
